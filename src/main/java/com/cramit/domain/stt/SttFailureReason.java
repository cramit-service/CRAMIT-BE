package com.cramit.domain.stt;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

//STT 실패 예외를 프론트에 알려줄 원인 코드로 바꾼다. 원인을 특정할 수 없으면 null이다.
final class SttFailureReason {

    private SttFailureReason() {
    }

    static ErrorCode of(Throwable ex) {
        if (ex instanceof BusinessException be) {
            return be.getErrorCode();
        }
        if (ex instanceof ResourceAccessException && hasTimeoutCause(ex.getCause())) {
            return ErrorCode.STT_TIMEOUT;
        }
        return null;
    }

    // IOException 등을 한 번 더 거쳐서 타임아웃 예외에 도달하는 경우도 있어, 원인 체인 전체를 확인한다.
    private static boolean hasTimeoutCause(Throwable cause) {
        for (Throwable t = cause; t != null; t = t.getCause()) {
            if (t instanceof HttpTimeoutException || t instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
