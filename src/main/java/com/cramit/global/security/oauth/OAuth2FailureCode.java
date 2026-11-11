package com.cramit.global.security.oauth;

/**
 * CustomOAuth2UserService가 던지는 OAuth2Error 코드 중, OAuth2AuthenticationFailureHandler가
 * 리다이렉트 쿼리파라미터(error)로 그대로 전달해 FE와 구분해서 안내하기로 한 코드 목록.
 * 그 외 코드(제공자 동의 취소 등 Spring/제공자 쪽 코드)는 기존처럼 일반 오류로 뭉뚱그린다.
 */
final class OAuth2FailureCode {

	static final String MEMBER_WITHDRAWN = "member_withdrawn";
	static final String AUTH_PROVIDER_UNAVAILABLE = "auth_provider_unavailable";
	static final String MEMBER_CREATE_FAILED = "member_create_failed";

	private OAuth2FailureCode() {
	}

}
