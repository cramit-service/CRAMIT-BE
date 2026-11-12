package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiEmbedRequest;
import com.cramit.domain.ai.dto.GeminiEmbedResponse;
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
 * RAG 2단계용 텍스트 임베딩 클라이언트.
 *
 * ⚠️ embedContent 요청·응답 필드명은 Gemini 공식 문서 기준으로 작성했지만, 실제 라이브 호출로 아직
 * 검증하지 못했다 (문서 간 taskType 위치가 top-level/embedContentConfig로 갈려 있음). 머지 전 실제
 * API 키로 한 번 호출해서 응답 스키마가 {@link GeminiEmbedResponse}와 맞는지 확인이 필요하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiEmbeddingClient {

    public static final String MODEL = "gemini-embedding-001";
    // pgvector 컬럼을 768차원으로 고정했으므로 여기도 맞춘다 (V19 마이그레이션 참고).
    public static final int OUTPUT_DIMENSIONALITY = 768;

    private final RestClient geminiRestClient;

    @Retry(name = "gemini", fallbackMethod = "fallbackDocument")
    @CircuitBreaker(name = "gemini")
    public float[] embedDocument(String title, String text) {
        GeminiEmbedRequest request = GeminiEmbedRequest.ofDocument(MODEL, title, text, OUTPUT_DIMENSIONALITY);
        return call(request);
    }

    @Retry(name = "gemini", fallbackMethod = "fallbackQuery")
    @CircuitBreaker(name = "gemini")
    public float[] embedQuery(String text) {
        GeminiEmbedRequest request = GeminiEmbedRequest.ofQuery(MODEL, text, OUTPUT_DIMENSIONALITY);
        return call(request);
    }

    private float[] call(GeminiEmbedRequest request) {
        GeminiEmbedResponse response = geminiRestClient.post()
                .uri("/v1beta/models/{model}:embedContent", MODEL)
                .body(request)
                .retrieve()
                .body(GeminiEmbedResponse.class);

        if (response == null) {
            throw new BusinessException(ErrorCode.EMBEDDING_GENERATION_FAILED);
        }
        return response.toFloatArray();
    }

    // CircuitBreaker가 open되거나 재시도 소진 시 호출되는 fallback. resilience4j는 원본 메서드와
    // 동일한 파라미터 타입(+Throwable)을 요구해서 embedDocument/embedQuery 각각 따로 둔다.
    private float[] fallbackDocument(String title, String text, Throwable ex) {
        log.warn("Gemini 임베딩(문서) 호출 실패: {}", ex.getMessage(), ex);
        throw new BusinessException(toErrorCode(ex), ex);
    }

    private float[] fallbackQuery(String text, Throwable ex) {
        log.warn("Gemini 임베딩(질의) 호출 실패: {}", ex.getMessage(), ex);
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
        return ErrorCode.EMBEDDING_GENERATION_FAILED;
    }
}
