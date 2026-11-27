package com.cramit.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME_NAME = "bearerAuth";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Cramit API")
						.description("강의자료(PDF)와 강의 녹음(STT)을 AI로 분석해 요약본과 학습 계획을 제공하는 Cramit의 백엔드 API 문서입니다.")
						.version("v1"))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
				.components(new Components()
						.addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
								.name(BEARER_SCHEME_NAME)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("로그인 후 발급받은 Access Token을 입력하세요 (Bearer 접두어 없이 토큰만 입력)")));
	}

}
