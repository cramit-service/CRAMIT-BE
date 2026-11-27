package com.cramit.global.security.oauth;

import com.cramit.domain.member.AuthService;
import com.cramit.domain.member.dto.TokenResponse;
import com.cramit.global.security.RefreshTokenCookieProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final AuthService authService;
	private final RefreshTokenCookieProvider refreshTokenCookieProvider;

	@Value("${app.oauth2.success-redirect-uri}")
	private String successRedirectUri;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException {
		CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
		TokenResponse tokens = authService.issueTokens(oAuth2User.getMemberId());

		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(tokens.refreshToken()).toString());

		// accessToken은 쿼리 파라미터가 아니라 URL fragment(#)로 보낸다. 쿼리 파라미터와 달리 fragment는
		// 브라우저가 Referer 헤더에 절대 포함시키지 않아서, 리다이렉트 도착 페이지가 토큰을 URL에서 지우기 전에
		// 제3자 리소스를 로드해도 Referer로 토큰이 새어나가지 않는다. isNewUser는 민감하지 않으니 그대로 쿼리로 둔다.
		String redirectUri = UriComponentsBuilder.fromUriString(successRedirectUri)
				.queryParam("isNewUser", oAuth2User.isOnboardingRequired())
				.build()
				.toUriString()
				+ "#accessToken=" + URLEncoder.encode(tokens.accessToken(), StandardCharsets.UTF_8);

		response.sendRedirect(redirectUri);
	}

}
