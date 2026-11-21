package com.cramit.domain.stt;

import com.cramit.domain.stt.dto.AzureTranscriptionResponse;
import com.cramit.domain.stt.dto.Transcription;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AzureSttClient {

    private static final String API_VERSION = "2025-10-15";
    private static final String DEFINITION = "{\"locales\":[\"ko-KR\"],\"profanityFilterMode\":\"None\"}";
    private static final int MAX_AUDIO_BYTES = 25 * 1024 * 1024; // FileType.AUDIO 업로드 한도

    private final RestClient azureSpeechRestClient;
    private final StorageFileDownloader storageFileDownloader;

    public byte[] download(String fileUrl) {
        return storageFileDownloader.download(fileUrl, MAX_AUDIO_BYTES);
    }

    public Transcription transcribe(String fileUrl, byte[] audio) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("audio", new ByteArrayResource(audio) {
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
}
