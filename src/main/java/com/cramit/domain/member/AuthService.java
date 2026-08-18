package com.cramit.domain.member;

import com.cramit.domain.member.dto.TokenResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.security.JwtTokenProvider;
import com.cramit.global.security.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenProvider jwtTokenProvider;

	public TokenResponse issueTokens(Long memberId) {
		return new TokenResponse(
				jwtTokenProvider.createAccessToken(memberId),
				jwtTokenProvider.createRefreshToken(memberId));
	}

	public TokenResponse reissueAccessToken(String refreshToken) {
		if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isTokenType(refreshToken, TokenType.REFRESH)) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
		}

		Long memberId = jwtTokenProvider.getMemberId(refreshToken);
		return new TokenResponse(jwtTokenProvider.createAccessToken(memberId), refreshToken);
	}

}
