package com.cramit.domain.stt;

import com.cramit.domain.stt.dto.GroqTranscriptionResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GroqSttClient {

    public static final String MODEL = "whisper-large-v3-turbo";
    private static final int MAX_AUDIO_BYTES = 25 * 1024 * 1024; // Groq 업로드 한도

    private final RestClient groqRestClient;
    private final RestClient audioDownloadRestClient;
    private final SttDownloadProperties sttDownloadProperties;

    public GroqTranscriptionResponse transcribe(String fileUrl) {
        validateFileUrl(fileUrl);

        byte[] audio = download(fileUrl);

        // Groq는 파일명 확장자로 오디오 형식을 판별한다.
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        form.add("model", MODEL);
        form.add("language", "ko");
        form.add("response_format", "verbose_json");

        return groqRestClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(GroqTranscriptionResponse.class);
    }

    private byte[] download(String fileUrl) {
        return audioDownloadRestClient.get()
                .uri(fileUrl)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("녹음 다운로드 실패: " + response.getStatusCode());
                    }
                    if (response.getHeaders().getContentLength() > MAX_AUDIO_BYTES) {
                        throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
                    }
                    // Content-Length가 없을 수도 있어서 한도 + 1바이트까지만 읽고 넘으면 멈춘다.
                    byte[] audio = response.getBody().readNBytes(MAX_AUDIO_BYTES + 1);
                    if (audio.length > MAX_AUDIO_BYTES) {
                        throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
                    }
                    return audio;
                });
    }

    // 사용자가 넘긴 fileUrl로 서버가 요청을 보내므로, 우리 저장소 공개 주소 아래만 받는다.
    private void validateFileUrl(String fileUrl) {
        String prefix = sttDownloadProperties.allowedUrlPrefix();
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalStateException("stt.download.allowed-url-prefix가 설정되지 않았습니다.");
        }
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        if (!fileUrl.startsWith(normalizedPrefix)) {
            throw new IllegalArgumentException("허용되지 않은 녹음 주소입니다: " + fileUrl);
        }
    }
}
