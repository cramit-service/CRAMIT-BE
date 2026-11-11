package com.cramit.global.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cramit.domain.member.Member;
import com.cramit.domain.member.MemberRepository;
import com.cramit.domain.member.SocialProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class CustomOAuth2UserServiceTest {

	private static final OAuth2UserRequest KAKAO_REQUEST = kakaoUserRequest();

	private final OAuth2ProviderUserInfoFetcher fetcher = mock(OAuth2ProviderUserInfoFetcher.class);
	private final MemberRepository memberRepository = mock(MemberRepository.class);
	private final CustomOAuth2UserService service = new CustomOAuth2UserService(fetcher, memberRepository);

	private static OAuth2UserRequest kakaoUserRequest() {
		ClientRegistration registration = ClientRegistration.withRegistrationId("kakao")
				.clientId("client-id")
				.clientSecret("secret")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("http://localhost:8080/login/oauth2/code/kakao")
				.authorizationUri("http://kakao.test/oauth/authorize")
				.tokenUri("http://kakao.test/oauth/token")
				.userInfoUri("http://kakao.test/v2/user/me")
				.userNameAttributeName("id")
				.build();
		OAuth2AccessToken accessToken = new OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER, "token-value", Instant.now(), Instant.now().plusSeconds(3600));
		return new OAuth2UserRequest(registration, accessToken);
	}

	private static OAuth2User kakaoOAuth2User(String socialId) {
		Map<String, Object> profile = Map.of("nickname", "서윤", "profile_image_url", "http://image");
		Map<String, Object> kakaoAccount = Map.of("profile", profile);
		Map<String, Object> attributes = Map.of("id", socialId, "kakao_account", kakaoAccount);
		return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
	}

	@Test
	void 기존_회원이면_그대로_로그인된다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "1", "http://image");
		when(fetcher.fetch(KAKAO_REQUEST)).thenReturn(kakaoOAuth2User("1"));
		when(memberRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "1"))
				.thenReturn(Optional.of(member));

		OAuth2User result = service.loadUser(KAKAO_REQUEST);

		assertThat(result).isInstanceOf(CustomOAuth2User.class);
	}

	@Test
	void OAuth_제공자_호출이_재시도와_서킷브레이커까지_소진돼_실패하면_AUTH_PROVIDER_UNAVAILABLE로_변환된다() {
		when(fetcher.fetch(KAKAO_REQUEST)).thenThrow(new RuntimeException("provider timeout"));

		assertThatThrownBy(() -> service.loadUser(KAKAO_REQUEST))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
				.isEqualTo(OAuth2FailureCode.AUTH_PROVIDER_UNAVAILABLE);
	}

	@Test
	void 회원_조회_저장_중_DB_오류가_나면_MEMBER_CREATE_FAILED로_변환된다() {
		when(fetcher.fetch(KAKAO_REQUEST)).thenReturn(kakaoOAuth2User("2"));
		when(memberRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "2"))
				.thenThrow(new DataAccessResourceFailureException("db down"));

		assertThatThrownBy(() -> service.loadUser(KAKAO_REQUEST))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
				.isEqualTo(OAuth2FailureCode.MEMBER_CREATE_FAILED);
	}

	@Test
	void 탈퇴한_회원이면_MEMBER_WITHDRAWN으로_변환된다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "3", "http://image");
		member.withdraw();
		when(fetcher.fetch(KAKAO_REQUEST)).thenReturn(kakaoOAuth2User("3"));
		when(memberRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "3"))
				.thenReturn(Optional.of(member));

		assertThatThrownBy(() -> service.loadUser(KAKAO_REQUEST))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
				.isEqualTo(OAuth2FailureCode.MEMBER_WITHDRAWN);
	}

}
