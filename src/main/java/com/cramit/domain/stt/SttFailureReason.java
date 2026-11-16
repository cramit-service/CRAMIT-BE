package com.cramit.domain.stt;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.external.ExternalTimeouts;

//STT 실패 예외를 프론트에 알려줄 원인 코드로 바꾼다. 원인을 특정할 수 없으면 null이다.
final class SttFailureReason {

    private SttFailureReason() {
    }

    static ErrorCode of(Throwable ex) {
        if (ex instanceof BusinessException be) {
            return be.getErrorCode();
        }
        if (ExternalTimeouts.isTimeout(ex)) {
            return ErrorCode.STT_TIMEOUT;
        }
        return null;
    }
}
