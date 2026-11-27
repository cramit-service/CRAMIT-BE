package com.cramit.domain.stt;

import com.cramit.domain.file.enums.FileType;
import com.cramit.domain.stt.dto.AzureTranscriptionResponse;
import com.cramit.domain.stt.dto.Transcription;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AzureSttClient {

    private static final String API_VERSION = "2025-10-15";
    private static final String DEFINITION = "{\"locales\":[\"ko-KR\"],\"profanityFilterMode\":\"None\"}";
    private static final Set<String> MP4_EXTENSIONS = Set.of("m4a", "mp4");

    private final RestClient azureSpeechRestClient;
    private final StorageFileDownloader storageFileDownloader;

    /** 녹음을 임시 파일로 받아 전사할 수 있는 모양으로 만든다. 다 쓰면 호출한 쪽이 지운다. */
    public Path download(String fileUrl) {
        Path audio = createTempFile("stt-audio-", ".tmp");
        try {
            storageFileDownloader.downloadTo(fileUrl, FileType.AUDIO.getMaxBytes(), audio);
            if (isMp4(fileName(fileUrl))) {
                moveIndexToFront(audio);
            }
            return audio;
        } catch (RuntimeException e) {
            deleteQuietly(audio);
            throw e;
        }
    }

    // 스케일업하는 동안에는 한도 안에서도 429가 날 수 있어서 재시도한다.
    @Retry(name = "azure-stt")
    public Transcription transcribe(String fileUrl, Path audio) {
        String fileName = fileName(fileUrl);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("audio", new FileSystemResource(audio) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        form.add("definition", DEFINITION);

        AzureTranscriptionResponse response = azureSpeechRestClient.post()
                .uri("/speechtotext/transcriptions:transcribe?api-version={version}", API_VERSION)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(AzureTranscriptionResponse.class);
        return response.toTranscription();
    }

    private static String fileName(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
    }

    private static boolean isMp4(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return MP4_EXTENSIONS.contains(extension);
    }

    // 녹음 앱은 moov(색인)를 파일 끝에 두는 경우가 많은데, Azure는 그런 파일을 422 InvalidAudioFormat으로 거절한다.
    // 다시 인코딩하지 않고 색인만 앞으로 옮긴다. faststart는 출력 파일을 되감아 써야 해서 파이프 대신 임시 파일을 쓴다.
    static void moveIndexToFront(Path audio) {
        Path target = createTempFile("stt-faststart-", ".mp4");
        try {
            Process process = new ProcessBuilder(
                    "ffmpeg", "-v", "error", "-y",
                    "-i", audio.toString(),
                    "-vn", "-c:a", "copy", "-movflags", "+faststart", "-f", "mp4", target.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("ffmpeg faststart 변환 실패: exitCode=" + exitCode);
            }
            Files.move(target, audio, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ffmpeg faststart 변환 중단", e);
        } finally {
            deleteQuietly(target);
        }
    }

    private static Path createTempFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
