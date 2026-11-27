package com.cramit.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcTaskDecoratorTest {

	@Test
	void worker_thread_inherits_mdc_from_submitting_thread() throws Exception {
		MDC.put("traceId", "test-trace-id");
		AtomicReference<String> traceIdSeenByWorker = new AtomicReference<>();

		try {
			Runnable decorated = new MdcTaskDecorator()
					.decorate(() -> traceIdSeenByWorker.set(MDC.get("traceId")));

			CompletableFuture.runAsync(decorated).get();

			assertThat(traceIdSeenByWorker.get()).isEqualTo("test-trace-id");
		} finally {
			MDC.clear();
		}
	}

}
