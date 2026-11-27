package com.cramit.domain.member;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProvisionalNicknameTest {

	@Test
	void 같은_표시_닉네임을_가진_서로_다른_소셜_계정은_겹치지_않는_임시_닉네임을_받는다() {
		String nicknameA = ProvisionalNickname.generate(SocialProvider.KAKAO, "111");
		String nicknameB = ProvisionalNickname.generate(SocialProvider.KAKAO, "222");

		assertThat(nicknameA).isNotEqualTo(nicknameB);
	}

	@Test
	void 임시_닉네임_형식과_일치하는_닉네임을_판별한다() {
		assertThat(ProvisionalNickname.matchesFormat("kakao_111")).isTrue();
		assertThat(ProvisionalNickname.matchesFormat("google_abcde")).isTrue();
	}

	@Test
	void 임시_닉네임_형식이_아닌_닉네임은_통과시킨다() {
		assertThat(ProvisionalNickname.matchesFormat("배서윤")).isFalse();
		assertThat(ProvisionalNickname.matchesFormat(null)).isFalse();
	}

}
