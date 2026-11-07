package com.cramit.global.security.oauth;

import com.cramit.domain.member.Member;
import com.cramit.domain.member.MemberRepository;
import com.cramit.domain.member.SocialProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final MemberRepository memberRepository;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		OAuthAttributes oAuthAttributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

		Member member = memberRepository
				.findBySocialProviderAndSocialId(oAuthAttributes.socialProvider(), oAuthAttributes.socialId())
				.orElseGet(() -> memberRepository.save(Member.ofSocialSignup(
						provisionalNickname(oAuthAttributes.socialProvider(), oAuthAttributes.socialId()),
						oAuthAttributes.socialProvider(),
						oAuthAttributes.socialId(),
						oAuthAttributes.profileImageUrl())));

		if (member.isDeleted()) {
			throw new OAuth2AuthenticationException(new OAuth2Error("member_withdrawn"), "탈퇴한 계정입니다.");
		}

		return new CustomOAuth2User(
				member.getId(),
				!member.isOnboardingCompleted(),
				oAuth2User.getAttributes(),
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	// 소셜 제공자 닉네임은 다른 회원과 겹칠 수 있어, 유니크 닉네임 제약에 걸리지 않는 값으로 가입시킨다.
	// 실제 닉네임은 온보딩(completeOnboarding)에서 다시 받는다.
	private String provisionalNickname(SocialProvider socialProvider, String socialId) {
		return socialProvider.name().toLowerCase() + "_" + socialId;
	}

}
