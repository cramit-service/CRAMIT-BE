package com.cramit.global.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class SampleExternalApiClientTest {

	@Autowired
	private SampleExternalApiClient client;

	@Autowired
	private FlakyExternalApiOperations flakyOperations;

	@Test
	void retries_and_recovers_from_transient_failures() {
		// resilience4j.retry.instances.stt.max-attempts = 3 이므로 2번 실패 후 3번째에 성공해야 함
		flakyOperations.failNextCalls(2);

		String result = client.call();

		assertThat(result).isEqualTo("ok");
		assertThat(flakyOperations.getInvocationCount()).isEqualTo(3);
	}

	@TestConfiguration
	static class FlakyClientTestConfig {

		@Bean
		@Primary
		public FlakyExternalApiOperations flakyExternalApiOperations() {
			return new FlakyExternalApiOperations();
		}

	}

	static class FlakyExternalApiOperations implements ExternalApiOperations {

		private final AtomicInteger invocationCount = new AtomicInteger();
		private int failuresRemaining = 0;

		void failNextCalls(int times) {
			this.failuresRemaining = times;
		}

		int getInvocationCount() {
			return invocationCount.get();
		}

		@Override
		public String execute() {
			invocationCount.incrementAndGet();
			if (failuresRemaining > 0) {
				failuresRemaining--;
				throw new RuntimeException("simulated transient failure");
			}
			return "ok";
		}

	}

}
