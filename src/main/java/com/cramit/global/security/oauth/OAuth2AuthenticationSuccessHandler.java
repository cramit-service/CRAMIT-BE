package com.cramit.global.security.oauth;

import com.cramit.domain.member.AuthService;
import com.cramit.domain.member.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final AuthService authService;

	@Value("${app.oauth2.success-redirect-uri}")
	private String successRedirectUri;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException {
		CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
		TokenResponse tokens = authService.issueTokens(oAuth2User.getMemberId());

		String redirectUri = UriComponentsBuilder.fromUriString(successRedirectUri)
				.queryParam("accessToken", tokens.accessToken())
				.queryParam("refreshToken", tokens.refreshToken())
				.build()
				.toUriString();

		response.sendRedirect(redirectUri);
	}

}
