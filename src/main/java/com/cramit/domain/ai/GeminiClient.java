package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiRequest;
import com.cramit.domain.ai.dto.GeminiResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

/**
 * 모든 Gemini 호출이 거치는 공통 클라이언트. 호출, 재시도·서킷브레이커, 오류 변환, 로깅을 맡는다.
 * 프롬프트와 응답 해석은 기능별 클라이언트(챗봇, 요약, TODO)가 맡는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    public static final String MODEL = "gemini-3.6-flash";

    private final RestClient geminiRestClient;
    private final RestClient geminiSummaryRestClient;

    public enum Timeout {
        DEFAULT, // 응답 대기 20초
        LONG     // 응답 대기 90초 (요약처럼 입력이 긴 요청)
    }

    /**
     * 요청을 보내고 응답을 그대로 돌려준다. 응답 본문이 없으면 null이다.
     * 재시도가 끝나도 실패하면 429는 AI_RATE_LIMITED, 타임아웃은 AI_TIMEOUT, 나머지는 failureCode로 바꿔 던진다.
     */
    @Retry(name = "gemini", fallbackMethod = "fallback")
    @CircuitBreaker(name = "gemini")
    public GeminiResponse generate(GeminiRequest request, Timeout timeout, ErrorCode failureCode) {
        long startedAt = System.currentTimeMillis();
        GeminiResponse response = restClient(timeout).post()
                .uri("/v1beta/models/{model}:generateContent", MODEL)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        // 프롬프트에는 사용자 학습 자료가 들어 있어서 내용은 남기지 않고 크기와 소요 시간만 남긴다.
        log.info("Gemini 호출 완료: model={}, timeout={}, 입력 {}자, {}ms",
                MODEL, timeout, inputLength(request), System.currentTimeMillis() - startedAt);
        return response;
    }

    // CircuitBreaker가 open되거나 재시도 소진 시 호출되는 fallback
    private GeminiResponse fallback(GeminiRequest request, Timeout timeout, ErrorCode failureCode, Throwable ex) {
        log.warn("Gemini 호출 실패: {}, {}", failureCode, ex.getMessage(), ex);
        throw new BusinessException(toErrorCode(ex, failureCode), ex);
    }

    static ErrorCode toErrorCode(Throwable ex, ErrorCode failureCode) {
        if (ex instanceof HttpClientErrorException.TooManyRequests) {
            return ErrorCode.AI_RATE_LIMITED;
        }
        if (ex instanceof ResourceAccessException
                && (ex.getCause() instanceof HttpTimeoutException
                || ex.getCause() instanceof SocketTimeoutException)) {
            return ErrorCode.AI_TIMEOUT;
        }
        return failureCode;
    }

    private RestClient restClient(Timeout timeout) {
        return timeout == Timeout.LONG ? geminiSummaryRestClient : geminiRestClient;
    }

    private int inputLength(GeminiRequest request) {
        return request.contents().stream()
                .flatMap(content -> content.parts().stream())
                .mapToInt(part -> part.text() != null ? part.text().length() : 0)
                .sum();
    }
}
