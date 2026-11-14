package com.cramit.global.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JsonNullable<T>을 Jackson이 제대로 역/직렬화하도록 등록한다.
 * 이 모듈이 없으면 JsonNullable 필드가 생략된 경우에도 isPresent()가 항상 true로 잡힌다.
 */
@Configuration
public class JacksonConfig {

	@Bean
	public JsonNullableModule jsonNullableModule() {
		return new JsonNullableModule();
	}

}
