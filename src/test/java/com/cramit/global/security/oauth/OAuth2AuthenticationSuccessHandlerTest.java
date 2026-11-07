package com.cramit.global.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cramit.domain.member.AuthService;
import com.cramit.domain.member.dto.TokenResponse;
import com.cramit.global.security.JwtProperties;
import com.cramit.global.security.RefreshTokenCookieProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2AuthenticationSuccessHandlerTest {

	private final AuthService authService = mock(AuthService.class);
	private final RefreshTokenCookieProvider refreshTokenCookieProvider =
			new RefreshTokenCookieProvider(new JwtProperties("test-secret", 1000, 1000));
	private final OAuth2AuthenticationSuccessHandler handler =
			new OAuth2AuthenticationSuccessHandler(authService, refreshTokenCookieProvider);

	@Test
	void 온보딩이_필요한_회원은_리다이렉트_주소에_isNewUser가_true로_담긴다() throws Exception {
		ReflectionTestUtils.setField(handler, "successRedirectUri", "http://localhost:3000/oauth/callback");
		when(authService.issueTokens(1L)).thenReturn(new TokenResponse("access-token", "refresh-token"));
		CustomOAuth2User principal = new CustomOAuth2User(
				1L, true, Map.of("id", "1"), List.of(new SimpleGrantedAuthority("ROLE_USER")));

		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.onAuthenticationSuccess(
				new MockHttpServletRequest(),
				response,
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

		assertThat(response.getRedirectedUrl())
				.contains("accessToken=access-token")
				.contains("isNewUser=true");
	}

	@Test
	void 온보딩을_완료한_회원은_리다이렉트_주소에_isNewUser가_false로_담긴다() throws Exception {
		ReflectionTestUtils.setField(handler, "successRedirectUri", "http://localhost:3000/oauth/callback");
		when(authService.issueTokens(2L)).thenReturn(new TokenResponse("access-token", "refresh-token"));
		CustomOAuth2User principal = new CustomOAuth2User(
				2L, false, Map.of("id", "2"), List.of(new SimpleGrantedAuthority("ROLE_USER")));

		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.onAuthenticationSuccess(
				new MockHttpServletRequest(),
				response,
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

		assertThat(response.getRedirectedUrl()).contains("isNewUser=false");
	}

}
