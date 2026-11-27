package com.cramit.domain.stt;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
                    checkResponse(response, maxBytes);
                    // Content-Length가 없을 수도 있어서 한도 + 1바이트까지만 읽고 넘으면 멈춘다.
                    byte[] file = response.getBody().readNBytes(maxBytes + 1);
                    if (file.length > maxBytes) {
                        throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
                    }
                    return file;
                });
    }

    // 녹음은 수백 MB까지 올 수 있어서, 여러 건을 동시에 처리해도 힙이 버티도록 메모리에 올리지 않고 파일로 받는다.
    public void downloadTo(String fileUrl, long maxBytes, Path target) {
        validateFileUrl(fileUrl);
        audioDownloadRestClient.get()
                .uri(fileUrl)
                .exchange((request, response) -> {
                    checkResponse(response, maxBytes);
                    try (InputStream body = response.getBody(); OutputStream out = Files.newOutputStream(target)) {
                        return body.transferTo(new LimitedOutputStream(out, maxBytes));
                    }
                });
    }

    private void checkResponse(ClientHttpResponse response, long maxBytes) throws IOException {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("파일 다운로드 실패: " + response.getStatusCode());
        }
        if (response.getHeaders().getContentLength() > maxBytes) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    // Content-Length가 없을 수도 있어서 실제로 쓴 바이트로도 한도를 확인한다.
    private static final class LimitedOutputStream extends FilterOutputStream {

        private final long maxBytes;
        private long written;

        LimitedOutputStream(OutputStream out, long maxBytes) {
            super(out);
            this.maxBytes = maxBytes;
        }

        @Override
        public void write(int b) throws IOException {
            count(1);
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            count(len);
            out.write(b, off, len);
        }

        private void count(int len) {
            written += len;
            if (written > maxBytes) {
                throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
            }
        }
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
