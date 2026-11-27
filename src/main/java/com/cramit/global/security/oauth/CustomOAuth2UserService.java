package com.cramit.global.security.oauth;

import com.cramit.domain.member.Member;
import com.cramit.domain.member.MemberRepository;
import com.cramit.domain.member.ProvisionalNickname;
import com.cramit.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final OAuth2ProviderUserInfoFetcher oAuth2ProviderUserInfoFetcher;
	private final MemberRepository memberRepository;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User;
		try {
			oAuth2User = oAuth2ProviderUserInfoFetcher.fetch(userRequest);
		} catch (Exception e) {
			// 재시도·Circuit Breaker까지 모두 소진된 뒤에도 실패한 경우 = OAuth 제공자가 응답하지 않는 상황
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2FailureCode.AUTH_PROVIDER_UNAVAILABLE),
					ErrorCode.AUTH_PROVIDER_UNAVAILABLE.getMessage(), e);
		}

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		OAuthAttributes oAuthAttributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

		Member member;
		try {
			member = memberRepository
					.findBySocialProviderAndSocialId(oAuthAttributes.socialProvider(), oAuthAttributes.socialId())
					.orElseGet(() -> memberRepository.save(Member.ofSocialSignup(
							ProvisionalNickname.generate(oAuthAttributes.socialProvider(), oAuthAttributes.socialId()),
							oAuthAttributes.socialProvider(),
							oAuthAttributes.socialId(),
							oAuthAttributes.profileImageUrl())));
		} catch (DataAccessException e) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2FailureCode.MEMBER_CREATE_FAILED),
					ErrorCode.MEMBER_CREATE_FAILED.getMessage(), e);
		}

		if (member.isDeleted()) {
			throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2FailureCode.MEMBER_WITHDRAWN), "탈퇴한 계정입니다.");
		}

		return new CustomOAuth2User(
				member.getId(),
				!member.isOnboardingCompleted(),
				oAuth2User.getAttributes(),
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

}
