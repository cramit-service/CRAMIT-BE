package com.cramit.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cramit.domain.member.dto.TokenResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.security.JwtProperties;
import com.cramit.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

	private final JwtTokenProvider jwtTokenProvider =
			new JwtTokenProvider(new JwtProperties("test-jwt-secret-key-for-unit-tests-only-32bytes+", 3600000L, 1209600000L));
	private final AuthService authService = new AuthService(jwtTokenProvider);

	@Test
	void 로그인_성공시_access_refresh_토큰을_모두_발급한다() {
		TokenResponse tokens = authService.issueTokens(1L);

		assertThat(jwtTokenProvider.getMemberId(tokens.accessToken())).isEqualTo(1L);
		assertThat(jwtTokenProvider.getMemberId(tokens.refreshToken())).isEqualTo(1L);
	}

	@Test
	void refresh_토큰으로_access_토큰을_재발급하면_refresh_토큰은_그대로_유지된다() {
		TokenResponse issued = authService.issueTokens(1L);

		TokenResponse reissued = authService.reissueAccessToken(issued.refreshToken());

		assertThat(reissued.refreshToken()).isEqualTo(issued.refreshToken());
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

}
