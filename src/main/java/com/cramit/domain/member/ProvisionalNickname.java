package com.cramit.domain.member;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 소셜 가입 시 CustomOAuth2UserService가 부여하는 임시 닉네임의 형식을 한곳에서 생성/판별한다.
// 사용자가 온보딩에서 같은 형식("kakao_111")을 직접 선택하면, 같은 socialId로 가입하는 신규 회원이
// uq_member_nickname_active 위반으로 영구히 로그인할 수 없게 되므로, 이 형식은 실제 닉네임으로 금지한다.
public final class ProvisionalNickname {

	private static final Pattern FORMAT = Pattern.compile(
			"^(" + Arrays.stream(SocialProvider.values())
					.map(provider -> provider.name().toLowerCase())
					.collect(Collectors.joining("|"))
					+ ")_.+$");

	private ProvisionalNickname() {
	}

	public static String generate(SocialProvider socialProvider, String socialId) {
		return socialProvider.name().toLowerCase() + "_" + socialId;
	}

	public static boolean matchesFormat(String nickname) {
		return nickname != null && FORMAT.matcher(nickname).matches();
	}

}
