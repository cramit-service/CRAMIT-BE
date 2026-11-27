package com.cramit.global.security;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieProvider {

	public static final String COOKIE_NAME = "refreshToken";
	private static final String COOKIE_PATH = "/api/auth";

	private final JwtProperties jwtProperties;

	public RefreshTokenCookieProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	public ResponseCookie create(String refreshToken) {
		return baseCookie(refreshToken)
				.maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpiration()))
				.build();
	}

	public ResponseCookie expire() {
		return baseCookie("")
				.maxAge(0)
				.build();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(COOKIE_NAME, value)
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path(COOKIE_PATH);
	}

}
