package com.cramit.domain.stt;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class StorageFileDownloader {

    private final RestClient audioDownloadRestClient;
    private final SttDownloadProperties sttDownloadProperties;

    public byte[] download(String fileUrl, int maxBytes) {
        validateFileUrl(fileUrl);
        return audioDownloadRestClient.get()
                .uri(fileUrl)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("파일 다운로드 실패: " + response.getStatusCode());
                    }
                    if (response.getHeaders().getContentLength() > maxBytes) {
                        throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
                    }
                    // Content-Length가 없을 수도 있어서 한도 + 1바이트까지만 읽고 넘으면 멈춘다.
                    byte[] file = response.getBody().readNBytes(maxBytes + 1);
                    if (file.length > maxBytes) {
                        throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
                    }
                    return file;
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
            throw new IllegalArgumentException("허용되지 않은 파일 주소입니다: " + fileUrl);
        }
    }
}
