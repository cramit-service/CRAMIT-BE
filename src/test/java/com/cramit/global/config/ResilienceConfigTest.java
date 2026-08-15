package com.cramit.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * application.yml에 정의한 resilience4j 정책(OAuth 5s / STT 30s / Gemini 20s 타임아웃,
 * 최대 2회 재시도, 연속 5회 실패시 서킷 오픈)이 실제 레지스트리에 반영되는지 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
class ResilienceConfigTest {

	@Autowired
	private TimeLimiterRegistry timeLimiterRegistry;

	@Autowired
	private RetryRegistry retryRegistry;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Test
	void timeout_durations_match_the_policy() {
		assertThat(timeLimiterRegistry.timeLimiter("oauth").getTimeLimiterConfig().getTimeoutDuration())
				.isEqualTo(Duration.ofSeconds(5));
		assertThat(timeLimiterRegistry.timeLimiter("stt").getTimeLimiterConfig().getTimeoutDuration())
				.isEqualTo(Duration.ofSeconds(30));
		assertThat(timeLimiterRegistry.timeLimiter("gemini").getTimeLimiterConfig().getTimeoutDuration())
				.isEqualTo(Duration.ofSeconds(20));
	}

	@Test
	void retry_allows_up_to_two_retries() {
		// max-attempts = 3 => 최초 시도 1회 + 재시도 2회
		assertThat(retryRegistry.retry("stt").getRetryConfig().getMaxAttempts()).isEqualTo(3);
	}

	@Test
	void circuit_breaker_opens_after_five_consecutive_failures_for_one_minute() {
		var config = circuitBreakerRegistry.circuitBreaker("stt").getCircuitBreakerConfig();
		assertThat(config.getSlidingWindowSize()).isEqualTo(5);
		assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
		assertThat(config.getFailureRateThreshold()).isEqualTo(100f);
		assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(60).toMillis());
	}

}
