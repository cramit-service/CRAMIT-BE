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
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiSummaryClient {

    public static final String MODEL = "gemini-3.6-flash";

    private static final Map<String, Object> SUMMARY_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of("summary", Map.of("type", "STRING")),
            "required", List.of("summary")
    );

    private final RestClient geminiSummaryRestClient;

    /**
     * 프롬프트로 요약을 요청하고 마크다운 요약본을 반환한다.
     * 응답 JSON 파싱에 실패하면 AI_RESPONSE_PARSE_ERROR를 던진다. (파싱 재시도는 호출하는 쪽에서 처리)
     */
    @Retry(name = "gemini", fallbackMethod = "fallback")
    @CircuitBreaker(name = "gemini")
    public String summarize(String systemPrompt, String userText) {
        GeminiRequest request = GeminiRequest.of(systemPrompt, userText, SUMMARY_SCHEMA);

        GeminiResponse response = geminiSummaryRestClient.post()
                .uri("/v1beta/models/{model}:generateContent", MODEL)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        return response.extractField("summary", ErrorCode.AI_RESPONSE_PARSE_ERROR);
    }

    // CircuitBreaker가 open되거나 재시도 소진 시 호출되는 fallback
    private String fallback(String systemPrompt, String userText, Throwable ex) {
        log.warn("Gemini 요약 호출 실패: {}", ex.getMessage(), ex);
        throw new BusinessException(toErrorCode(ex), ex);
    }

    private ErrorCode toErrorCode(Throwable ex) {
        if (ex instanceof BusinessException be) {
            return be.getErrorCode();
        }
        if (ex instanceof HttpClientErrorException.TooManyRequests) {
            return ErrorCode.AI_RATE_LIMITED;
        }
        if (ex instanceof ResourceAccessException
                && (ex.getCause() instanceof HttpTimeoutException
                || ex.getCause() instanceof SocketTimeoutException)) {
            return ErrorCode.AI_TIMEOUT;
        }
        return ErrorCode.SUMMARY_GENERATION_FAILED;
    }
}
