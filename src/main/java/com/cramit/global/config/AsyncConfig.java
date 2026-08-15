package com.cramit.global.config;

import com.cramit.global.logging.MdcTaskDecorator;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * STT/AI요약/학습내용적용/TODO생성 등 비동기 작업이 공통으로 사용할 스레드풀 설정.
 * 각 도메인 서비스는 @Async("taskExecutor")로 이 executor를 사용한다.
 */
@Configuration
@EnableAsync
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

}
