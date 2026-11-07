package com.cramit.global.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.cramit.domain.member.MemberRepository;
import com.cramit.domain.member.SocialProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CustomOAuth2UserServiceTest {

	private final CustomOAuth2UserService service = new CustomOAuth2UserService(mock(MemberRepository.class));

	@Test
	void 같은_표시_닉네임을_가진_서로_다른_소셜_계정은_겹치지_않는_임시_닉네임을_받는다() {
		String nicknameA = ReflectionTestUtils.invokeMethod(service, "provisionalNickname", SocialProvider.KAKAO, "111");
		String nicknameB = ReflectionTestUtils.invokeMethod(service, "provisionalNickname", SocialProvider.KAKAO, "222");

		assertThat(nicknameA).isNotEqualTo(nicknameB);
	}

}
