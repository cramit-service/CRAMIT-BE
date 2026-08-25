package com.cramit.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

	@Value("${app.oauth2.failure-redirect-uri}")
	private String failureRedirectUri;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {
		String redirectUri = UriComponentsBuilder.fromUriString(failureRedirectUri)
				.queryParam("error", "oauth_login_failed")
				.build()
				.toUriString();

		response.sendRedirect(redirectUri);
	}

}
