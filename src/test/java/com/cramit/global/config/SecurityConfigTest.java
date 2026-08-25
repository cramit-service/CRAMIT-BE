package com.cramit.global.config;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cramit.global.security.JwtTokenProvider;
import com.cramit.global.security.RefreshTokenCookieProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void 인증되지_않은_요청은_401과_표준_에러_포맷을_반환한다() throws Exception {
		mockMvc.perform(get("/api/member/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void oauth2_authorization_요청은_카카오_인가_엔드포인트로_리다이렉트된다() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/kakao"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", startsWith("https://kauth.kakao.com/oauth/authorize")));
	}

	@Test
	void oauth2_authorization_요청은_구글_인가_엔드포인트로_리다이렉트된다() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/google"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", startsWith("https://accounts.google.com/o/oauth2/v2/auth")));
	}

	@Test
	void refresh_쿠키가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/auth/refresh"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void 유효하지_않은_refresh_쿠키는_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
						.cookie(new Cookie(RefreshTokenCookieProvider.COOKIE_NAME, "invalid-token")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void 유효한_refresh_쿠키로_인증_없이_컨트롤러까지_도달해_access_토큰을_재발급받는다() throws Exception {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);

		mockMvc.perform(post("/api/auth/refresh")
						.cookie(new Cookie(RefreshTokenCookieProvider.COOKIE_NAME, refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists());
	}

}
