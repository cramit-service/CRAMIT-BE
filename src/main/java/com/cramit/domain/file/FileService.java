package com.cramit.domain.file;

import com.cramit.domain.file.dto.UploadUrlRequest;
import com.cramit.domain.file.dto.UploadUrlResponse;
import com.cramit.domain.file.enums.FileType;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
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
    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public UploadUrlResponse createUploadUrl(UploadUrlRequest request) {
        FileType fileType = request.fileType();
        String extension = extractExtension(request.fileName(), fileType);
        if (!fileType.allows(extension)) {
            throw new BusinessException(fileType.getInvalidFormatError());
        }
        if (request.fileSize() != null && request.fileSize() > fileType.getMaxBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
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

    /**
     * 주차에 등록할 파일이 업로드를 마쳤는지 확인한다.
     * 업로드 경로와 형식이 맞는지 보고, 저장소에 파일이 없으면 UPLOAD_INTERRUPTED, 용량을 넘으면 FILE_TOO_LARGE를 던진다.
     */
    public void verifyUploaded(String fileUrl, FileType fileType) {
        validateFileUrl(fileUrl);

        String key = fileUrl.substring(publicBaseUrl().length() + 1);
        if (!key.startsWith("files/" + fileType.getDirectory() + "/")) {
            throw new BusinessException(fileType.getInvalidFormatError());
        }

        HeadObjectResponse object;
        try {
            object = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(storageProperties.s3().bucket())
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.UPLOAD_INTERRUPTED);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new BusinessException(ErrorCode.UPLOAD_INTERRUPTED);
            }
            throw e;
        }

        if (object.contentLength() != null && object.contentLength() > fileType.getMaxBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    // 클라이언트가 보낸 fileUrl이 우리 저장소의 업로드 경로인지 확인한다.
    public void validateFileUrl(String fileUrl) {
        String allowedPrefix = publicBaseUrl() + "/files/";
        if (fileUrl == null || !fileUrl.startsWith(allowedPrefix)) {
            throw new BusinessException(ErrorCode.FILE_INVALID_URL);
        }
    }

    private String publicBaseUrl() {
        return stripTrailingSlash(storageProperties.s3().publicBaseUrl());
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String extractExtension(String fileName, FileType fileType) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new BusinessException(fileType.getInvalidFormatError());
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
