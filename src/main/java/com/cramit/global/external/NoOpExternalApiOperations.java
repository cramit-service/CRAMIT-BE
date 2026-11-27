package com.cramit.global.external;

import org.springframework.stereotype.Component;

/**
 * ExternalApiOperations의 기본(placeholder) 구현체.
 * 실제 STT/Gemini 등 도메인 클라이언트가 생기면 이 빈을 각자의 구현으로 대체하면 된다.
 */
@Component
public class NoOpExternalApiOperations implements ExternalApiOperations {

	@Override
	public String execute() {
		return "ok";
	}

}
