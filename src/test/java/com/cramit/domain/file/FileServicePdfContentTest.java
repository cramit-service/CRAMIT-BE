package com.cramit.domain.file;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// S3Presigner는 네트워크 없이 URL만 만들지만, 이 테스트가 다루는 메서드는 실제 S3Client 호출 결과를 흉내 내야
// 해서 @SpringBootTest의 실제(연결 안 되는) S3Client 대신 Mockito로 직접 구성한다.
@ExtendWith(MockitoExtension.class)
class FileServicePdfContentTest {

    private static final String FILE_URL = "http://localhost:9000/test-bucket/files/ppt/slide.pdf";
    private static final String KEY = "files/ppt/slide.pdf";

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Client s3Client;

    private final StorageProperties storageProperties = new StorageProperties(
            "access", "secret", "ap-northeast-2",
            new StorageProperties.S3("test-bucket", "", "http://localhost:9000/test-bucket"));

    // @Mock 필드는 각 테스트 실행 직전에 채워지므로, 필드 초기화 시점이 아니라 @BeforeEach에서 조립해야 한다.
    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(s3Presigner, s3Client, storageProperties);
    }

    @Test
    @DisplayName("PDF 시그니처와 50MB 이하 용량이면 검증된 ETag로 최종 키에 복사하고 그 fileUrl을 반환한다")
    void finalizePdfUpload_valid() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(1024L).eTag("\"etag-1\"").build());
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(pdfBodyStream());

        String finalFileUrl = fileService.finalizePdfUpload(FILE_URL);

        assertThat(finalFileUrl)
                .startsWith("http://localhost:9000/test-bucket/files/ppt/")
                .endsWith(".pdf")
                .isNotEqualTo(FILE_URL);
    }

    @Test
    @DisplayName("검증 시점의 ETag와 다르면(TOCTOU) 복사가 거부되어 FILE_UPLOAD_CONFLICT")
    void finalizePdfUpload_etagChangedBeforeCopy() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(1024L).eTag("\"etag-1\"").build());
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(pdfBodyStream());
        when(s3Client.copyObject(any(CopyObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("precondition failed").statusCode(412).build());

        assertThatThrownBy(() -> fileService.finalizePdfUpload(FILE_URL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_UPLOAD_CONFLICT);
    }

    @Test
    @DisplayName("50MB를 넘으면 FILE_TOO_LARGE")
    void finalizePdfUpload_tooLarge() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(51L * 1024 * 1024).build());

        assertThatThrownBy(() -> fileService.finalizePdfUpload(FILE_URL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("PDF 시그니처가 아니면(확장자만 바꾼 파일) FILE_INVALID_TYPE")
    void finalizePdfUpload_notPdf() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(1024L).build());
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(bodyStream(new byte[]{'P', 'K', 3, 4, 0}));

        assertThatThrownBy(() -> fileService.finalizePdfUpload(FILE_URL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("S3에 실제로 업로드되지 않은 키면 FILE_INVALID_URL")
    void finalizePdfUpload_notUploaded() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("no such key").build());

        assertThatThrownBy(() -> fileService.finalizePdfUpload(FILE_URL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_INVALID_URL);
    }

    @Test
    @DisplayName("시그니처 길이보다 작은 파일은 Range 조회 없이 바로 FILE_INVALID_TYPE")
    void finalizePdfUpload_tooSmallToHaveSignature() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(3L).build());

        assertThatThrownBy(() -> fileService.finalizePdfUpload(FILE_URL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("S3 장애(5xx 등)로 조회에 실패하면 URL 문제가 아니라 INTERNAL_SERVER_ERROR")
    void finalizePdfUpload_s3ServiceFailure() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("service unavailable").statusCode(503).build());

        assertThatThrownBy(() -> fileService.finalizePdfUpload(FILE_URL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("저장소 업로드 경로가 아닌 fileUrl이면 S3를 조회하지 않고 바로 FILE_INVALID_URL")
    void finalizePdfUpload_invalidUrl() {
        assertThatThrownBy(() -> fileService.finalizePdfUpload("http://169.254.169.254/files/ppt/slide.pdf"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_INVALID_URL);
    }

    private ResponseInputStream<GetObjectResponse> pdfBodyStream() {
        return bodyStream(new byte[]{'%', 'P', 'D', 'F', '-'});
    }

    private ResponseInputStream<GetObjectResponse> bodyStream(byte[] bytes) {
        return new ResponseInputStream<>(GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(bytes)));
    }
}
