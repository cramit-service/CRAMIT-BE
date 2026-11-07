package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiRequest;
import com.cramit.domain.ai.dto.GeminiResponse;
import com.cramit.domain.ai.dto.GeminiTodoItem;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class GeminiTodoClient {

    public static final String MODEL = "gemini-3.6-flash";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Map<String, Object> TODO_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of("todos", Map.of(
                    "type", "ARRAY",
                    "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "weekId", Map.of("type", "INTEGER"),
                                    "content", Map.of("type", "STRING"),
                                    "dueDate", Map.of("type", "STRING"),
                                    "memo", Map.of("type", "STRING")
                            ),
                            "required", List.of("weekId", "content", "dueDate")
                    )
            )),
            "required", List.of("todos")
    );

    private final RestClient geminiSummaryRestClient;

    @Retry(name = "gemini", fallbackMethod = "fallback")
    @CircuitBreaker(name = "gemini")
    public List<GeminiTodoItem> generate(String systemPrompt, String userText) {
        GeminiRequest request = GeminiRequest.of(systemPrompt, userText, TODO_SCHEMA);

        GeminiResponse response = geminiSummaryRestClient.post()
                .uri("/v1beta/models/{model}:generateContent", MODEL)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null) {
            throw new BusinessException(ErrorCode.TODO_GENERATION_FAILED);
        }

        try {
            TodoPlan plan = OBJECT_MAPPER.readValue(response.extractText(ErrorCode.TODO_GENERATION_FAILED), TodoPlan.class);
            return plan.todos() != null ? plan.todos() : List.of();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TODO_GENERATION_FAILED);
        }
    }

    private List<GeminiTodoItem> fallback(String systemPrompt, String userText, Throwable ex) {
        log.warn("Gemini 학습 계획 호출 실패: {}", ex.getMessage(), ex);
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
        return ErrorCode.TODO_GENERATION_FAILED;
    }

    private record TodoPlan(List<GeminiTodoItem> todos) {}
}
