package com.cramit.global.config;

import com.cramit.global.logging.MdcTaskDecorator;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * STT/AI요약/학습내용적용/TODO생성 등 비동기 작업이 공통으로 사용할 스레드풀 설정.
 * 각 도메인 서비스는 @Async("taskExecutor")로 이 executor를 사용한다.
 * 임베딩 백필 스케줄러(PageEmbeddingBackfillScheduler)의 @Scheduled도 여기서 활성화한다.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

	@Override
	@Bean(name = "taskExecutor")
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("async-task-");
		executor.setTaskDecorator(new MdcTaskDecorator());
		// 서버 종료 시 실행 중/대기 중인 작업을 끝까지 처리하고 종료 (최대 30초 대기)
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.initialize();
		return executor;
	}

	// STT 한 건은 전사·교정·매핑으로 수 분간 스레드를 잡아서, 공용 풀에 두면 요약·TODO 생성까지 밀린다.
	// 대부분 외부 API 응답 대기라 동시 사용자 수만큼 스레드를 둔다. 큐가 차기 전에는 core를 넘겨 늘리지 않으므로 core = max.
	@Bean(name = "sttExecutor")
	public Executor sttExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("stt-");
		executor.setTaskDecorator(new MdcTaskDecorator());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.initialize();
		return executor;
	}

}
