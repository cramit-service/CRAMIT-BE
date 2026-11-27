package com.cramit.global.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.cramit.domain.member.SocialProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OAuthAttributesTest {

	@Test
	void 카카오_응답에서_닉네임_프로필사진_id를_추출한다() {
		Map<String, Object> attributes = Map.of(
				"id", 123456789L,
				"kakao_account", Map.of(
						"profile", Map.of(
								"nickname", "서윤",
								"profile_image_url", "https://kakao.example/profile.jpg"
						)
				)
		);

		OAuthAttributes result = OAuthAttributes.of("kakao", attributes);

		assertThat(result.socialProvider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(result.socialId()).isEqualTo("123456789");
		assertThat(result.nickname()).isEqualTo("서윤");
		assertThat(result.profileImageUrl()).isEqualTo("https://kakao.example/profile.jpg");
	}

	@Test
	void 구글_응답에서_이름_프로필사진_id를_추출한다() {
		Map<String, Object> attributes = Map.of(
				"id", "google-user-id",
				"name", "서윤",
				"picture", "https://google.example/profile.jpg"
		);

		OAuthAttributes result = OAuthAttributes.of("google", attributes);

		assertThat(result.socialProvider()).isEqualTo(SocialProvider.GOOGLE);
		assertThat(result.socialId()).isEqualTo("google-user-id");
		assertThat(result.nickname()).isEqualTo("서윤");
		assertThat(result.profileImageUrl()).isEqualTo("https://google.example/profile.jpg");
	}

}
