package com.cramit.global.security.oauth;

import com.cramit.domain.member.SocialProvider;
import java.util.Map;

public record OAuthAttributes(
		String nickname,
		String profileImageUrl,
		SocialProvider socialProvider,
		String socialId
) {

	public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
		if ("kakao".equalsIgnoreCase(registrationId)) {
			return ofKakao(attributes);
		}
		return ofGoogle(attributes);
	}

	@SuppressWarnings("unchecked")
	private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
		Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
		Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

		return new OAuthAttributes(
				(String) profile.get("nickname"),
				(String) profile.get("profile_image_url"),
				SocialProvider.KAKAO,
				String.valueOf(attributes.get("id"))
		);
	}

	private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
		return new OAuthAttributes(
				(String) attributes.get("name"),
				(String) attributes.get("picture"),
				SocialProvider.GOOGLE,
				String.valueOf(attributes.get("id"))
		);
	}

}
