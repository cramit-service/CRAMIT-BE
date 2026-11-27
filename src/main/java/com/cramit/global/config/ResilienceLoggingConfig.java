package com.cramit.global.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 API(OAuth/STT/Gemini) 호출 실패/재시도/Circuit Breaker 상태 변화를 구조화 로그로 남긴다.
 * 실제 응답코드 로깅은 각 클라이언트 호출부에서 예외 메시지에 담아 남기는 것을 권장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilienceLoggingConfig {

	private final CircuitBreakerRegistry circuitBreakerRegistry;
	private final RetryRegistry retryRegistry;

	@PostConstruct
	public void registerEventListeners() {
		circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerCircuitBreakerEvents);
		circuitBreakerRegistry.getEventPublisher()
				.onEntryAdded(event -> registerCircuitBreakerEvents(event.getAddedEntry()));

		retryRegistry.getAllRetries().forEach(retry ->
				retry.getEventPublisher().onRetry(event ->
						log.warn("[Retry] name={} attempt={} lastException={}",
								retry.getName(), event.getNumberOfRetryAttempts(), event.getLastThrowable().toString())));
	}

	private void registerCircuitBreakerEvents(CircuitBreaker circuitBreaker) {
		circuitBreaker.getEventPublisher()
				.onStateTransition(event -> log.warn("[CircuitBreaker] name={} transition={}",
						circuitBreaker.getName(), event.getStateTransition()))
				.onError(event -> log.error("[CircuitBreaker] name={} call failed. duration={}ms cause={}",
						circuitBreaker.getName(), event.getElapsedDuration().toMillis(), event.getThrowable().toString()));
	}

}
