package com.cramit.domain.stt;

import com.cramit.domain.file.enums.FileType;
import com.cramit.domain.stt.dto.AzureTranscriptionResponse;
import com.cramit.domain.stt.dto.Transcription;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public byte[] download(String fileUrl) {
        return storageFileDownloader.download(fileUrl, Math.toIntExact(FileType.AUDIO.getMaxBytes()));
    }

    public Transcription transcribe(String fileUrl, byte[] audio) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        byte[] body = isMp4(fileName) ? moveIndexToFront(audio) : audio;
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("audio", new ByteArrayResource(body) {
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

    private static boolean isMp4(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return MP4_EXTENSIONS.contains(extension);
    }

    // 녹음 앱은 moov(색인)를 파일 끝에 두는 경우가 많은데, Azure는 그런 파일을 422 InvalidAudioFormat으로 거절한다.
    // 다시 인코딩하지 않고 색인만 앞으로 옮긴다. faststart는 출력 파일을 되감아 써야 해서 파이프 대신 임시 파일을 쓴다.
    static byte[] moveIndexToFront(byte[] audio) {
        Path source = null;
        Path target = null;
        try {
            source = Files.createTempFile("stt-mp4-", ".tmp");
            target = Files.createTempFile("stt-faststart-", ".mp4");
            Files.write(source, audio);
            Process process = new ProcessBuilder(
                    "ffmpeg", "-v", "error", "-y",
                    "-i", source.toString(),
                    "-vn", "-c:a", "copy", "-movflags", "+faststart", "-f", "mp4", target.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("ffmpeg faststart 변환 실패: exitCode=" + exitCode);
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ffmpeg faststart 변환 중단", e);
        } finally {
            deleteQuietly(source);
            deleteQuietly(target);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
