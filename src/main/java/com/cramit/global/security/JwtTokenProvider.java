package com.cramit.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
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
		return parseIfValid(token).isPresent();
	}

	public boolean isTokenType(String token, TokenType tokenType) {
		return parseIfValid(token)
				.map(claims -> isTokenType(claims, tokenType))
				.orElse(false);
	}

	public Long getMemberId(String token) {
		return memberIdOf(parseClaims(token));
	}

	/**
	 * 서명/만료를 한 번만 검증하고 재사용 가능한 Claims를 반환한다.
	 * 유효하지 않은 토큰이면 빈 값을 반환한다 (매 호출마다 재파싱하지 않도록 호출부에서 이 결과를 재사용할 것).
	 */
	public Optional<Claims> parseIfValid(String token) {
		try {
			return Optional.of(parseClaims(token));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public boolean isTokenType(Claims claims, TokenType tokenType) {
		return tokenType.name().equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
	}

	public Long memberIdOf(Claims claims) {
		return Long.valueOf(claims.getSubject());
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
