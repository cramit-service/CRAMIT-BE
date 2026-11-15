package com.cramit.global.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2AuthenticationFailureHandlerTest {

	private final OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler();

	@Test
	void CustomOAuth2UserService가_구분해둔_코드는_그대로_리다이렉트_파라미터에_담긴다() throws Exception {
		ReflectionTestUtils.setField(handler, "failureRedirectUri", "http://localhost:3000/oauth/callback");
		OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
				new OAuth2Error(OAuth2FailureCode.AUTH_PROVIDER_UNAVAILABLE), "provider unavailable");

		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.onAuthenticationFailure(new MockHttpServletRequest(), response, exception);

		assertThat(response.getRedirectedUrl()).contains("error=" + OAuth2FailureCode.AUTH_PROVIDER_UNAVAILABLE);
	}

	@Test
	void 구분하지_않는_코드는_기존처럼_일반_오류로_뭉뚱그린다() throws Exception {
		ReflectionTestUtils.setField(handler, "failureRedirectUri", "http://localhost:3000/oauth/callback");
		OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
				new OAuth2Error("access_denied"), "user cancelled consent");

		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.onAuthenticationFailure(new MockHttpServletRequest(), response, exception);

		assertThat(response.getRedirectedUrl()).contains("error=oauth_login_failed");
	}

	@Test
	void OAuth2예외가_아닌_인증예외도_일반_오류로_처리한다() throws Exception {
		ReflectionTestUtils.setField(handler, "failureRedirectUri", "http://localhost:3000/oauth/callback");

		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.onAuthenticationFailure(new MockHttpServletRequest(), response, new BadCredentialsException("bad"));

		assertThat(response.getRedirectedUrl()).contains("error=oauth_login_failed");
	}

}
