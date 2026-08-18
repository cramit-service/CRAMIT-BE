package com.cramit.global.config;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

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
	void refresh_요청은_인증_없이_컨트롤러까지_도달한다() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"invalid-token\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
	}

}
