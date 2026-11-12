package com.cramit.domain.file;

import com.cramit.domain.file.dto.UploadUrlRequest;
import com.cramit.domain.file.dto.UploadUrlResponse;
import com.cramit.domain.file.enums.FileType;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
    // presigned PUT URL은 만료(5분) 전까지 재사용 가능해서, PDF는 이 스테이징 경로에만 올리게 하고
    // 검증을 통과한 내용만 PPT 디렉터리로 서버사이드 복사해서 등록한다 (#93, TOCTOU 방지).
    private static final String STAGING_PPT_DIRECTORY = "ppt/staging";

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public UploadUrlResponse createUploadUrl(UploadUrlRequest request) {
        String extension = extractExtension(request.fileName());
        if (!request.fileType().allows(extension)) {
            throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
        }

        // 원본 파일명은 등록 API가 따로 저장하므로 key에는 넣지 않는다 (한글·공백이 URL 인코딩 문제를 일으킴)
        // PDF(PPT)는 내용 검증 전까지 스테이징 경로에만 올린다 (#93) - 검증되지 않은 키를 바로 등록하지 않기 위함.
        String directory = request.fileType() == FileType.PPT ? STAGING_PPT_DIRECTORY : request.fileType().getDirectory();
        String key = "files/" + directory + "/" + UUID.randomUUID() + "." + extension;

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
    // 검증을 통과한 내용만 PPT 디렉터리로 서버사이드 복사해서 등록하고, 그 최종 fileUrl을 반환한다.
    // presigned PUT URL은 만료 전까지 재사용 가능해서 검증과 등록 사이에 스테이징 키가 덮어써질 수 있는데,
    // 복사를 검증 시점의 ETag에 조건부(copySourceIfMatch)로 걸어서 그 TOCTOU를 막는다 (#93).
    public String finalizePdfUpload(String stagingFileUrl) {
        validateStagingPptUrl(stagingFileUrl);
        String bucket = storageProperties.s3().bucket();
        String stagingKey = extractKey(stagingFileUrl);
        // 로그에만 쓴다. key(= fileUrl 유래, 사용자 입력)에 CR/LF가 섞여 있으면 로그 라인을 위조할 수 있어서 이스케이프한다.
        String logKey = escapeForLog(stagingKey);

        HeadObjectResponse head;
        try {
            head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(stagingKey)
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

        // ifMatch로 headObject 때 확인한 ETag와 다르면 412로 실패시켜, 시그니처를 검증한 바이트와
        // 뒤에서 copySourceIfMatch로 복사할 객체가 반드시 같은 버전이 되게 한다 (#93 - 검증과 복사 사이 TOCTOU 방지).
        byte[] header;
        try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(stagingKey)
                .range("bytes=0-4")
                .ifMatch(head.eTag())
                .build())) {
            header = in.readNBytes(PDF_MAGIC_BYTES.length);
        } catch (S3Exception e) {
            if (e.statusCode() == 412) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_CONFLICT);
            }
            // headObject로 존재를 이미 확인했으므로, 여기서 나는 다른 오류는 파일 형식이 아니라 읽기/저장소 쪽 문제다.
            log.error("PDF 검증 중 S3 getObject 실패. key={}", logKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            log.error("PDF 검증 중 S3 getObject 실패. key={}", logKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (!Arrays.equals(header, PDF_MAGIC_BYTES)) {
            throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
        }

        String finalKey = "files/" + FileType.PPT.getDirectory() + "/" + UUID.randomUUID() + ".pdf";
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(stagingKey)
                    .destinationBucket(bucket)
                    .destinationKey(finalKey)
                    .copySourceIfMatch(head.eTag())
                    .build());
        } catch (S3Exception e) {
            if (e.statusCode() == 412) {
                // 검증 이후 같은 스테이징 키에 다른 파일이 덮어써진 경우 (TOCTOU)
                throw new BusinessException(ErrorCode.FILE_UPLOAD_CONFLICT);
            }
            log.error("PDF 확정 복사 실패. key={}", logKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return stripTrailingSlash(storageProperties.s3().publicBaseUrl()) + "/" + finalKey;
    }

    // finalizePdfUpload가 복사해간 스테이징 원본을 정리한다. DB 저장이 끝난 뒤에만 호출해야 한다 -
    // 저장이 실패했는데 먼저 지워버리면 재시도할 원본이 없어진다 (#93 CodeRabbit 피드백).
    public void deleteStagingPdf(String stagingFileUrl) {
        validateStagingPptUrl(stagingFileUrl);
        String stagingKey = extractKey(stagingFileUrl);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(storageProperties.s3().bucket())
                    .key(stagingKey)
                    .build());
        } catch (S3Exception e) {
            // 스테이징 파일 정리가 실패해도 등록 자체엔 영향 없어서 경고만 남긴다.
            log.warn("스테이징 파일 삭제 실패. key={}", escapeForLog(stagingKey), e);
        }
    }

    // finalizePdfUpload/deleteStagingPdf의 입력은 반드시 스테이징 경로여야 한다. 이미 확정된 PPT
    // fileUrl(다른 주차 것 포함)이나 다른 파일 유형의 경로가 들어오면 그 객체를 복사·삭제해버릴 수 있어 거부한다 (#93).
    private void validateStagingPptUrl(String fileUrl) {
        String allowedPrefix = stripTrailingSlash(storageProperties.s3().publicBaseUrl()) + "/files/" + STAGING_PPT_DIRECTORY + "/";
        if (fileUrl == null || !fileUrl.startsWith(allowedPrefix)) {
            throw new BusinessException(ErrorCode.FILE_INVALID_URL);
        }
    }

    private String extractKey(String fileUrl) {
        return fileUrl.substring((stripTrailingSlash(storageProperties.s3().publicBaseUrl()) + "/").length());
    }

    private String escapeForLog(String key) {
        return key.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
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
