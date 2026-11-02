package com.cramit.domain.stt;

import com.cramit.domain.stt.dto.GroqTranscriptionResponse;
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

    private final RestClient groqRestClient;
    private final RestClient audioDownloadRestClient;

    public GroqTranscriptionResponse transcribe(String fileUrl) {
        byte[] audio = audioDownloadRestClient.get()
                .uri(fileUrl)
                .retrieve()
                .body(byte[].class);

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
}
