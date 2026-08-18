package com.cramit.global.security.oauth;

import com.cramit.domain.member.Member;
import com.cramit.domain.member.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
						oAuthAttributes.nickname(),
						oAuthAttributes.socialProvider(),
						oAuthAttributes.socialId(),
						oAuthAttributes.profileImageUrl())));

		return new CustomOAuth2User(
				member.getId(),
				oAuth2User.getAttributes(),
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

}
