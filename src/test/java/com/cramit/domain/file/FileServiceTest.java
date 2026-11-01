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
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class FileServiceTest {

    @Autowired
    private FileService fileService;

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
        assertThatThrownBy(() -> fileService.createUploadUrl(new UploadUrlRequest("noextension", FileType.AUDIO)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_INVALID_TYPE);
    }
}
