package com.cramit.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final String CLAIM_TOKEN_TYPE = "tokenType";

	private final JwtProperties jwtProperties;
	private final SecretKey key;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(Long memberId) {
		return createToken(memberId, TokenType.ACCESS, jwtProperties.accessTokenExpiration());
	}

	public String createRefreshToken(Long memberId) {
		return createToken(memberId, TokenType.REFRESH, jwtProperties.refreshTokenExpiration());
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public boolean isTokenType(String token, TokenType tokenType) {
		return tokenType.name().equals(parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class));
	}

	public Long getMemberId(String token) {
		return Long.valueOf(parseClaims(token).getSubject());
	}

	private String createToken(Long memberId, TokenType tokenType, long expirationMillis) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMillis);

		return Jwts.builder()
				.subject(String.valueOf(memberId))
				.claim(CLAIM_TOKEN_TYPE, tokenType.name())
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

}
