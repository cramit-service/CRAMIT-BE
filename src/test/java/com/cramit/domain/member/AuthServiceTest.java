package com.cramit.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cramit.domain.member.dto.TokenResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.security.JwtProperties;
import com.cramit.global.security.JwtTokenProvider;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthServiceTest {

	private final JwtTokenProvider jwtTokenProvider =
			new JwtTokenProvider(new JwtProperties("test-jwt-secret-key-for-unit-tests-only-32bytes+", 3600000L, 1209600000L));
	private final JwtProperties jwtProperties =
			new JwtProperties("test-jwt-secret-key-for-unit-tests-only-32bytes+", 3600000L, 1209600000L);
	private final MemberRepository memberRepository = mock(MemberRepository.class);

	private final Map<String, String> redisStore = new HashMap<>();
	private final RedisTemplate<String, String> redisTemplate = fakeRedisTemplate(redisStore);

	private final AuthService authService =
			new AuthService(jwtTokenProvider, jwtProperties, memberRepository, redisTemplate);

	@Test
	void 로그인_성공시_access_refresh_토큰을_모두_발급한다() {
		TokenResponse tokens = authService.issueTokens(1L);

		assertThat(jwtTokenProvider.getMemberId(tokens.accessToken())).isEqualTo(1L);
		assertThat(jwtTokenProvider.getMemberId(tokens.refreshToken())).isEqualTo(1L);
	}

	@Test
	void refresh_토큰으로_새로운_access_토큰을_재발급한다() {
		TokenResponse issued = authService.issueTokens(1L);

		TokenResponse reissued = authService.reissueAccessToken(issued.refreshToken());

		assertThat(jwtTokenProvider.getMemberId(reissued.accessToken())).isEqualTo(1L);
		assertThat(jwtTokenProvider.getMemberId(reissued.refreshToken())).isEqualTo(1L);
	}

	@Test
	void 재발급_받으면_이전_refresh_토큰은_더이상_사용할_수_없다() {
		TokenResponse issued = authService.issueTokens(1L);
		authService.reissueAccessToken(issued.refreshToken());

		assertThatThrownBy(() -> authService.reissueAccessToken(issued.refreshToken()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
	}

	@Test
	void 로그아웃하면_refresh_토큰을_재사용할_수_없다() {
		TokenResponse issued = authService.issueTokens(1L);
		authService.logout(1L);

		assertThatThrownBy(() -> authService.reissueAccessToken(issued.refreshToken()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
	}

	@Test
	void 다른_회원이_발급받은_토큰으로는_재발급을_요청할_수_없다() {
		authService.issueTokens(1L);
		TokenResponse other = authService.issueTokens(2L);
		authService.logout(2L);

		assertThatThrownBy(() -> authService.reissueAccessToken(other.refreshToken()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
	}

	@Test
	void 다른_회원이_토큰을_발급받아도_기존_회원의_refresh_토큰은_유효하다() {
		TokenResponse first = authService.issueTokens(1L);
		authService.issueTokens(2L);

		TokenResponse reissued = authService.reissueAccessToken(first.refreshToken());

		assertThat(jwtTokenProvider.getMemberId(reissued.accessToken())).isEqualTo(1L);
	}

	@Test
	void access_토큰으로는_재발급을_요청할_수_없다() {
		TokenResponse issued = authService.issueTokens(1L);

		assertThatThrownBy(() -> authService.reissueAccessToken(issued.accessToken()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
	}

	@Test
	void 유효하지_않은_토큰으로는_재발급을_요청할_수_없다() {
		assertThatThrownBy(() -> authService.reissueAccessToken("not-a-valid-jwt"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
	}

	@Test
	void 회원_탈퇴시_deleted_플래그가_true로_바뀐다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

		authService.withdraw(1L);

		assertThat(member.isDeleted()).isTrue();
	}

	@Test
	void 회원_탈퇴하면_refresh_토큰을_재사용할_수_없다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		TokenResponse issued = authService.issueTokens(1L);

		authService.withdraw(1L);

		assertThatThrownBy(() -> authService.reissueAccessToken(issued.refreshToken()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
	}

	@Test
	void 존재하지_않는_회원은_탈퇴할_수_없다() {
		when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.withdraw(999L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
	}

	@SuppressWarnings("unchecked")
	private static RedisTemplate<String, String> fakeRedisTemplate(Map<String, String> store) {
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(valueOperations.get(anyString())).thenAnswer(invocation -> store.get(invocation.getArgument(0)));
		doAnswer(invocation -> store.put(invocation.getArgument(0), invocation.getArgument(1)))
				.when(valueOperations).set(anyString(), anyString(), any(Duration.class));

		RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.delete(anyString())).thenAnswer(invocation -> store.remove(invocation.getArgument(0)) != null);

		return redisTemplate;
	}

}
