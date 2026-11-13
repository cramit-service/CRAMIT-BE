package com.cramit.domain.stt;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class SttFailureReasonTest {

    @Test
    @DisplayName("응답 대기 시간을 넘기면 STT_TIMEOUT")
    void timeout() {
        assertThat(SttFailureReason.of(new ResourceAccessException("timeout", new HttpTimeoutException("timeout"))))
                .isEqualTo(ErrorCode.STT_TIMEOUT);
        assertThat(SttFailureReason.of(new ResourceAccessException("timeout", new SocketTimeoutException("timeout"))))
                .isEqualTo(ErrorCode.STT_TIMEOUT);
    }

    @Test
    @DisplayName("업무 예외는 그 오류 코드를 그대로 쓴다. (예: 녹음이 25MB를 넘으면 FILE_TOO_LARGE)")
    void businessException() {
        assertThat(SttFailureReason.of(new BusinessException(ErrorCode.FILE_TOO_LARGE)))
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("원인을 특정할 수 없으면 null")
    void unknown() {
        assertThat(SttFailureReason.of(new ResourceAccessException("connection reset", new IOException("reset")))).isNull();
        assertThat(SttFailureReason.of(new IllegalStateException("녹음 다운로드 실패"))).isNull();
    }
}
