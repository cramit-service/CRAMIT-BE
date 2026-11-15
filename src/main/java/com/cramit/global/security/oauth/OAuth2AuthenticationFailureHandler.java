package com.cramit.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

	private static final String DEFAULT_ERROR = "oauth_login_failed";
	// CustomOAuth2UserService가 직접 던지는, FE와 구분해서 안내하기로 한 코드만 그대로 전달한다.
	// 그 외(제공자 동의 취소 등 Spring/제공자 쪽 코드)는 기존처럼 DEFAULT_ERROR로 뭉뚱그린다.
	private static final Set<String> RECOGNIZED_ERRORS = Set.of(
			OAuth2FailureCode.MEMBER_WITHDRAWN,
			OAuth2FailureCode.AUTH_PROVIDER_UNAVAILABLE,
			OAuth2FailureCode.MEMBER_CREATE_FAILED);

	@Value("${app.oauth2.failure-redirect-uri}")
	private String failureRedirectUri;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {
		String errorCode = DEFAULT_ERROR;
		if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
			String code = oAuth2Exception.getError().getErrorCode();
			if (RECOGNIZED_ERRORS.contains(code)) {
				errorCode = code;
			}
		}

		String redirectUri = UriComponentsBuilder.fromUriString(failureRedirectUri)
				.queryParam("error", errorCode)
				.build()
				.toUriString();

		response.sendRedirect(redirectUri);
	}

}
