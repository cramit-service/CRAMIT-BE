package com.cramit.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private final JwtProperties jwtProperties =
			new JwtProperties("test-jwt-secret-key-for-unit-tests-only-32bytes+", 3600000L, 1209600000L);
	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties);

	@Test
	void access_token으로_memberId를_추출할_수_있다() {
		String accessToken = jwtTokenProvider.createAccessToken(1L);

		assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
		assertThat(jwtTokenProvider.getMemberId(accessToken)).isEqualTo(1L);
		assertThat(jwtTokenProvider.isTokenType(accessToken, TokenType.ACCESS)).isTrue();
		assertThat(jwtTokenProvider.isTokenType(accessToken, TokenType.REFRESH)).isFalse();
	}

	@Test
	void refresh_token은_REFRESH_타입으로_발급된다() {
		String refreshToken = jwtTokenProvider.createRefreshToken(1L);

		assertThat(jwtTokenProvider.isTokenType(refreshToken, TokenType.REFRESH)).isTrue();
	}

	@Test
	void 만료된_토큰은_유효하지_않다() {
		JwtProperties expiredTokenProperties =
				new JwtProperties("test-jwt-secret-key-for-unit-tests-only-32bytes+", -1000L, -1000L);
		JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(expiredTokenProperties);

		String expiredToken = expiredTokenProvider.createAccessToken(1L);

		assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
	}

	@Test
	void 서명이_다른_토큰은_유효하지_않다() {
		JwtProperties otherSecretProperties =
				new JwtProperties("another-jwt-secret-key-for-unit-tests-only-32bytes+", 3600000L, 1209600000L);
		JwtTokenProvider otherTokenProvider = new JwtTokenProvider(otherSecretProperties);

		String tokenSignedByOther = otherTokenProvider.createAccessToken(1L);

		assertThat(jwtTokenProvider.validateToken(tokenSignedByOther)).isFalse();
	}

	@Test
	void 형식이_잘못된_토큰은_유효하지_않다() {
		assertThat(jwtTokenProvider.validateToken("not-a-valid-jwt")).isFalse();
	}

}
