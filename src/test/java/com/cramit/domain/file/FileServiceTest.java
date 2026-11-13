package com.cramit.domain.file;

import com.cramit.domain.file.dto.UploadUrlRequest;
import com.cramit.domain.file.dto.UploadUrlResponse;
import com.cramit.domain.file.enums.FileType;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
class FileServiceTest {

    private static final String AUDIO_URL = "http://localhost:9000/test-bucket/files/audio/lecture.m4a";

    @Autowired
    private FileService fileService;

    @MockBean
    private S3Client s3Client;

    @Test
    @DisplayName("업로드 URL과 같은 key를 가리키는 공개 fileUrl을 발급한다")
    void createUploadUrl() {
        UploadUrlResponse response = fileService.createUploadUrl(new UploadUrlRequest("알고리즘 5주차.PDF", FileType.PPT));

        assertThat(response.fileUrl()).matches("http://localhost:9000/test-bucket/files/ppt/[0-9a-f-]{36}\\.pdf");
        String key = response.fileUrl().substring("http://localhost:9000/test-bucket/".length());
        assertThat(response.uploadUrl()).startsWith("http://localhost:9000/test-bucket/" + key + "?");
        assertThat(response.uploadUrl()).contains("X-Amz-Signature=");
        assertThat(response.expiresIn()).isEqualTo(300);
    }

    @Test
    @DisplayName("AUDIO는 Groq가 받는 확장자로 발급한다")
    void createUploadUrl_audio() {
        UploadUrlResponse response = fileService.createUploadUrl(new UploadUrlRequest("5주차 녹음.m4a", FileType.AUDIO));

        assertThat(response.fileUrl()).matches("http://localhost:9000/test-bucket/files/audio/[0-9a-f-]{36}\\.m4a");
    }

    @Test
    @DisplayName("파일 유형에 맞지 않는 확장자는 FILE_INVALID_TYPE")
    void createUploadUrl_invalidExtension() {
        assertThatThrownBy(() -> fileService.createUploadUrl(new UploadUrlRequest("lecture.mp3", FileType.PPT)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("음성 파일의 확장자가 맞지 않으면 AUDIO_INVALID_FORMAT")
    void createUploadUrl_invalidAudioFormat() {
        assertThatThrownBy(() -> fileService.createUploadUrl(new UploadUrlRequest("lecture.pdf", FileType.AUDIO)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUDIO_INVALID_FORMAT);
        assertThatThrownBy(() -> fileService.createUploadUrl(new UploadUrlRequest("noextension", FileType.AUDIO)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUDIO_INVALID_FORMAT);
    }

    @Test
    @DisplayName("파일 크기를 보냈고 한도를 넘으면 FILE_TOO_LARGE")
    void createUploadUrl_tooLarge() {
        long overLimit = FileType.AUDIO.getMaxBytes() + 1;

        assertThatThrownBy(() -> fileService.createUploadUrl(new UploadUrlRequest("lecture.m4a", FileType.AUDIO, overLimit)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("업로드된 파일이 저장소에 있고 한도 이내면 통과")
    void verifyUploaded() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder().contentLength(1024L).build());

        fileService.verifyUploaded(AUDIO_URL, FileType.AUDIO);
    }

    @Test
    @DisplayName("저장소에 파일이 없으면 업로드가 끝나지 않은 것으로 보고 UPLOAD_INTERRUPTED")
    void verifyUploaded_missing() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willThrow(NoSuchKeyException.builder().statusCode(404).build());

        assertThatThrownBy(() -> fileService.verifyUploaded(AUDIO_URL, FileType.AUDIO))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UPLOAD_INTERRUPTED);
    }

    @Test
    @DisplayName("저장소의 실제 파일이 한도를 넘으면 FILE_TOO_LARGE")
    void verifyUploaded_tooLarge() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder().contentLength(FileType.AUDIO.getMaxBytes() + 1).build());

        assertThatThrownBy(() -> fileService.verifyUploaded(AUDIO_URL, FileType.AUDIO))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("PDF 업로드 경로를 음성으로 등록하면 AUDIO_INVALID_FORMAT")
    void verifyUploaded_wrongType() {
        assertThatThrownBy(() -> fileService.verifyUploaded("http://localhost:9000/test-bucket/files/ppt/slide.pdf", FileType.AUDIO))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUDIO_INVALID_FORMAT);
    }

    @Test
    @DisplayName("저장소 업로드 경로의 주소는 통과")
    void validateFileUrl() {
        fileService.validateFileUrl("http://localhost:9000/test-bucket/files/audio/lecture.m4a");
    }

    @Test
    @DisplayName("저장소 업로드 경로가 아닌 주소는 FILE_INVALID_URL")
    void validateFileUrl_invalid() {
        assertThatThrownBy(() -> fileService.validateFileUrl("http://169.254.169.254/files/audio/lecture.m4a"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_INVALID_URL);
    }
}
