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

import com.cramit.domain.subscription.Plan;
import com.cramit.domain.subscription.PlanRepository;
import com.cramit.domain.subscription.PlanType;
import com.cramit.domain.term.Term;
import com.cramit.domain.term.TermRepository;
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

	@Autowired
	private TermRepository termRepository;

	@Autowired
	private PlanRepository planRepository;

	@Test
	void 인증된_회원은_자신의_프로필을_조회하고_수정하고_탈퇴할_수_있다() throws Exception {
		Member member = memberRepository.save(
				Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", "https://example.com/old.jpg"));
		String accessToken = jwtTokenProvider.createAccessToken(member.getId());

		mockMvc.perform(get("/api/users/me/profile")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nickname").value("서윤"))
				.andExpect(jsonPath("$.data.onboardingCompleted").value(false));

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

	@Test
	void 닉네임_중복확인을_할_수_있다() throws Exception {
		Member member = memberRepository.save(Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null));
		String accessToken = jwtTokenProvider.createAccessToken(member.getId());

		mockMvc.perform(get("/api/users/check-nickname")
						.queryParam("nickname", "서윤")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.available").value(false));

		mockMvc.perform(get("/api/users/check-nickname")
						.queryParam("nickname", "사용안한닉네임")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.available").value(true));
	}

	@Test
	void 필수_약관에_동의하고_요금제를_선택해_온보딩을_완료할_수_있다() throws Exception {
		Member member = memberRepository.save(
				Member.ofSocialSignup("카카오닉네임", SocialProvider.KAKAO, "12345", null));
		String accessToken = jwtTokenProvider.createAccessToken(member.getId());
		Term requiredTerm = termRepository.save(Term.builder().title("이용약관").content("내용").required(true).build());
		planRepository.save(Plan.builder().type(PlanType.FREE).price(0).description("무료").build());

		mockMvc.perform(post("/api/users/me/onboarding")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"배서윤\",\"agreedTermIds\":[" + requiredTerm.getId() + "],\"plan\":\"FREE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nickname").value("배서윤"))
				.andExpect(jsonPath("$.data.plan").value("FREE"));

		mockMvc.perform(get("/api/users/me/profile")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nickname").value("배서윤"))
				.andExpect(jsonPath("$.data.onboardingCompleted").value(true));
	}

	@Test
	void 필수_약관에_동의하지_않으면_온보딩할_수_없다() throws Exception {
		Member member = memberRepository.save(
				Member.ofSocialSignup("카카오닉네임", SocialProvider.KAKAO, "12345", null));
		String accessToken = jwtTokenProvider.createAccessToken(member.getId());
		termRepository.save(Term.builder().title("이용약관").content("내용").required(true).build());
		planRepository.save(Plan.builder().type(PlanType.FREE).price(0).description("무료").build());

		mockMvc.perform(post("/api/users/me/onboarding")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"배서윤\",\"agreedTermIds\":[],\"plan\":\"FREE\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("TERMS_NOT_AGREED"));
	}

	@Test
	void 이미_온보딩을_완료한_회원은_다시_온보딩할_수_없다() throws Exception {
		Member member = memberRepository.save(
				Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null));
		member.completeOnboarding("서윤");
		String accessToken = jwtTokenProvider.createAccessToken(member.getId());

		mockMvc.perform(post("/api/users/me/onboarding")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"배서윤\",\"agreedTermIds\":[],\"plan\":\"FREE\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ONBOARDING_ALREADY_COMPLETED"));
	}

	@Test
	void 탈퇴한_회원은_만료되지_않은_토큰으로도_온보딩할_수_없다() throws Exception {
		Member member = memberRepository.save(
				Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null));
		String accessToken = jwtTokenProvider.createAccessToken(member.getId());
		member.withdraw();
		memberRepository.save(member);

		mockMvc.perform(post("/api/users/me/onboarding")
						.header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nickname\":\"배서윤\",\"agreedTermIds\":[],\"plan\":\"FREE\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("MEMBER_WITHDRAWN"));
	}

	@Test
	void 약관_목록을_조회할_수_있다() throws Exception {
		Member member = memberRepository.save(Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null));
		String accessToken = jwtTokenProvider.createAccessToken(member.getId());
		termRepository.save(Term.builder().title("이용약관").content("내용").required(true).build());

		mockMvc.perform(get("/api/terms")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].title").value("이용약관"))
				.andExpect(jsonPath("$.data[0].required").value(true));
	}

	@Test
	void 약관_목록은_등록된_순서대로_조회된다() throws Exception {
		Member member = memberRepository.save(Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null));
		String accessToken = jwtTokenProvider.createAccessToken(member.getId());
		Term first = termRepository.save(Term.builder().title("이용약관").content("내용").required(true).build());
		Term second = termRepository.save(Term.builder().title("개인정보 수집 동의").content("내용").required(true).build());

		mockMvc.perform(get("/api/terms")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].termId").value(first.getId()))
				.andExpect(jsonPath("$.data[1].termId").value(second.getId()));
	}

}
