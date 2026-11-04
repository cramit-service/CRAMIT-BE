package com.cramit.domain.member;

import com.cramit.domain.member.dto.TokenResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.security.JwtProperties;
import com.cramit.global.security.JwtTokenProvider;
import com.cramit.global.security.TokenType;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh-token:";

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final MemberRepository memberRepository;
	private final RedisTemplate<String, String> redisTemplate;

	public TokenResponse issueTokens(Long memberId) {
		String accessToken = jwtTokenProvider.createAccessToken(memberId);
		String refreshToken = jwtTokenProvider.createRefreshToken(memberId);
		storeRefreshToken(memberId, refreshToken);
		return new TokenResponse(accessToken, refreshToken);
	}

	public TokenResponse reissueAccessToken(String refreshToken) {
		Optional<Claims> claims = jwtTokenProvider.parseIfValid(refreshToken);
		if (claims.isEmpty() || !jwtTokenProvider.isTokenType(claims.get(), TokenType.REFRESH)) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
		}

		Long memberId = jwtTokenProvider.memberIdOf(claims.get());
		String storedToken = redisTemplate.opsForValue().get(refreshTokenKey(memberId));
		if (storedToken == null || !storedToken.equals(refreshToken)) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
		}

		String newAccessToken = jwtTokenProvider.createAccessToken(memberId);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);
		storeRefreshToken(memberId, newRefreshToken);

		return new TokenResponse(newAccessToken, newRefreshToken);
	}

	public void logout(Long memberId) {
		redisTemplate.delete(refreshTokenKey(memberId));
	}

	@Transactional
	public void withdraw(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
		member.withdraw();
		redisTemplate.delete(refreshTokenKey(memberId));
	}

	private void storeRefreshToken(Long memberId, String refreshToken) {
		redisTemplate.opsForValue().set(
				refreshTokenKey(memberId),
				refreshToken,
				Duration.ofMillis(jwtProperties.refreshTokenExpiration()));
	}

	private String refreshTokenKey(Long memberId) {
		return REFRESH_TOKEN_KEY_PREFIX + memberId;
	}

}
