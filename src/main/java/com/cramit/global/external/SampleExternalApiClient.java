package com.cramit.global.external;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 외부 API 호출에 재시도(Retry) + Circuit Breaker를 적용하는 방법을 보여주는 예시 클라이언트.
 * 실제 STT/Gemini 클라이언트를 만들 때 이 패턴(어노테이션 + application.yml의 resilience4j 인스턴스명)을 그대로 가져다 쓰면 된다.
 * 타임아웃은 실제 HTTP 클라이언트(RestClient/WebClient)의 connect/read timeout으로 설정하고,
 * 비동기 호출을 CompletableFuture로 감싸는 시점에 @TimeLimiter(name = "stt")를 추가로 적용한다.
 */
@Component
@RequiredArgsConstructor
public class SampleExternalApiClient {

	private final ExternalApiOperations operations;

	@Retry(name = "stt")
	@CircuitBreaker(name = "stt")
	public String call() {
		return operations.execute();
	}

}
