package com.cramit.global.external;

import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * 외부 API 호출이 응답 대기 시간을 넘겨 실패했는지 판단한다.
 * JdkClientHttpRequestFactory의 읽기 제한 시간이 지나면 예외가 ResourceAccessException → IOException → TimeoutException처럼
 * 여러 겹으로 감싸져 오므로, 바로 아래 원인만 보지 않고 원인 체인 전체를 확인한다.
 */
public final class ExternalTimeouts {

    private ExternalTimeouts() {
    }

    public static boolean isTimeout(Throwable ex) {
        if (!(ex instanceof ResourceAccessException)) {
            return false;
        }
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException
                    || cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
