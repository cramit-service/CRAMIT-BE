package com.cramit.domain.file;

import com.cramit.domain.file.dto.UploadUrlRequest;
import com.cramit.domain.file.dto.UploadUrlResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Duration UPLOAD_URL_EXPIRATION = Duration.ofMinutes(5);

    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    public UploadUrlResponse createUploadUrl(UploadUrlRequest request) {
        String extension = extractExtension(request.fileName());
        if (!request.fileType().allows(extension)) {
            throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
        }

        // 원본 파일명은 등록 API가 따로 저장하므로 key에는 넣지 않는다 (한글·공백이 URL 인코딩 문제를 일으킴)
        String key = "files/" + request.fileType().getDirectory() + "/" + UUID.randomUUID() + "." + extension;

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_EXPIRATION)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(storageProperties.s3().bucket())
                        .key(key)
                        .build())
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        String fileUrl = stripTrailingSlash(storageProperties.s3().publicBaseUrl()) + "/" + key;

        return new UploadUrlResponse(uploadUrl, fileUrl, UPLOAD_URL_EXPIRATION.toSeconds());
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
