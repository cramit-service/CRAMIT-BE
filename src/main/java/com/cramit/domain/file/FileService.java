package com.cramit.domain.file;

import com.cramit.domain.file.dto.UploadUrlRequest;
import com.cramit.domain.file.dto.UploadUrlResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private static final Duration UPLOAD_URL_EXPIRATION = Duration.ofMinutes(5);
    private static final long MAX_PDF_SIZE_BYTES = 50L * 1024 * 1024;
    // PDF 파일 시그니처("%PDF-"). 확장자만 .pdf로 바꾼 다른 형식의 파일을 걸러낸다.
    private static final byte[] PDF_MAGIC_BYTES = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
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

    // 클라이언트가 보낸 fileUrl이 우리 저장소의 업로드 경로인지 확인한다.
    public void validateFileUrl(String fileUrl) {
        String allowedPrefix = stripTrailingSlash(storageProperties.s3().publicBaseUrl()) + "/files/";
        if (fileUrl == null || !fileUrl.startsWith(allowedPrefix)) {
            throw new BusinessException(ErrorCode.FILE_INVALID_URL);
        }
    }

    // presigned URL로 직접 올라간 파일이라 서버가 업로드 시점엔 내용을 못 본다. 등록 시점에
    // 실제 용량과 PDF 시그니처를 확인해, 확장자만 바꾼 파일이나 50MB를 넘는 파일을 걸러낸다.
    public void validatePdfContent(String fileUrl) {
        validateFileUrl(fileUrl);
        String key = fileUrl.substring((stripTrailingSlash(storageProperties.s3().publicBaseUrl()) + "/").length());
        // 로그에만 쓴다. key(= fileUrl 유래, 사용자 입력)에 CR/LF가 섞여 있으면 로그 라인을 위조할 수 있어서 이스케이프한다.
        String logKey = key.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");

        HeadObjectResponse head;
        try {
            head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(storageProperties.s3().bucket())
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            // 아직 업로드가 끝나지 않았거나 presigned URL을 쓰지 않고 가짜 fileUrl을 보낸 경우
            throw new BusinessException(ErrorCode.FILE_INVALID_URL);
        } catch (S3Exception e) {
            // 키가 없는 게 아니라 저장소 쪽 장애(5xx 등)인 경우. 사용자 URL 문제가 아니므로 구분해서 기록한다.
            log.error("PDF 검증 중 S3 headObject 실패. key={}", logKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (head.contentLength() != null && head.contentLength() > MAX_PDF_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        // 시그니처 길이보다 작은 파일은 Range 조회가 416(InvalidRange)으로 실패하므로, 조회 전에 형식 오류로 걸러낸다.
        if (head.contentLength() != null && head.contentLength() < PDF_MAGIC_BYTES.length) {
            throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
        }

        byte[] header;
        try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(GetObjectRequest.builder()
                .bucket(storageProperties.s3().bucket())
                .key(key)
                .range("bytes=0-4")
                .build())) {
            header = in.readNBytes(PDF_MAGIC_BYTES.length);
        } catch (IOException | S3Exception e) {
            // headObject로 존재를 이미 확인했으므로, 여기서 나는 오류는 파일 형식이 아니라 읽기/저장소 쪽 문제다.
            log.error("PDF 검증 중 S3 getObject 실패. key={}", logKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (!Arrays.equals(header, PDF_MAGIC_BYTES)) {
            throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
        }
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
