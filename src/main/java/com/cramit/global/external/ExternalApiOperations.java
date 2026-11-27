package com.cramit.global.external;

/**
 * 실제 외부 API(OAuth/STT/Gemini) 클라이언트가 구현할 인터페이스 예시.
 * 도메인별 실제 클라이언트를 만들 때 이 인터페이스 형태를 참고하면 된다.
 */
public interface ExternalApiOperations {

	String execute();

}
