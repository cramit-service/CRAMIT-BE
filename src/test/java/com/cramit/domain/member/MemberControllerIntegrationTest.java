package com.cramit.domain.member;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cramit.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void 인증된_회원은_자신의_프로필을_조회하고_수정하고_탈퇴할_수_있다() throws Exception {
		Member member = memberRepository.save(
				Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", "https://example.com/old.jpg"));
		String accessToken = jwtTokenProvider.createAccessToken(member.getId());

		mockMvc.perform(get("/api/users/me/profile")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nickname").value("서윤"));

		mockMvc.perform(patch("/api/users/me/profile")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"배서윤\",\"profileImageUrl\":\"https://example.com/new.jpg\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nickname").value("배서윤"))
				.andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/new.jpg"));

		mockMvc.perform(post("/api/auth/logout")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("refreshToken", 0))
				.andExpect(cookie().httpOnly("refreshToken", true))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=None")));

		mockMvc.perform(delete("/api/auth/withdraw")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNoContent());
	}

	@Test
	void 토큰_없이는_프로필을_조회할_수_없다() throws Exception {
		mockMvc.perform(get("/api/users/me/profile"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
	}

}
