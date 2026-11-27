package com.cramit.global.logging;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * @Async 작업이 요청 스레드의 MDC(traceId 포함)를 이어받도록 복사해준다.
 * 이게 없으면 taskExecutor에서 실행되는 비동기 작업의 로그에는 traceId가 안 찍힌다.
 */
public class MdcTaskDecorator implements TaskDecorator {

	@Override
	@NonNull
	public Runnable decorate(@NonNull Runnable runnable) {
		Map<String, String> contextMap = MDC.getCopyOfContextMap();
		return () -> {
			try {
				if (contextMap != null) {
					MDC.setContextMap(contextMap);
				}
				runnable.run();
			} finally {
				MDC.clear();
			}
		};
	}

}
