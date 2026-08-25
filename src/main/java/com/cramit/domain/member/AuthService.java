package com.cramit.domain.member;

import com.cramit.domain.member.dto.AccessTokenResponse;
import com.cramit.domain.member.dto.TokenResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.cramit.global.security.JwtTokenProvider;
import com.cramit.global.security.TokenType;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenProvider jwtTokenProvider;
	private final MemberRepository memberRepository;

	public TokenResponse issueTokens(Long memberId) {
		return new TokenResponse(
				jwtTokenProvider.createAccessToken(memberId),
				jwtTokenProvider.createRefreshToken(memberId));
	}

	public AccessTokenResponse reissueAccessToken(String refreshToken) {
		Optional<Claims> claims = jwtTokenProvider.parseIfValid(refreshToken);
		if (claims.isEmpty() || !jwtTokenProvider.isTokenType(claims.get(), TokenType.REFRESH)) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
		}

		Long memberId = jwtTokenProvider.memberIdOf(claims.get());
		return new AccessTokenResponse(jwtTokenProvider.createAccessToken(memberId));
	}

	@Transactional
	public void withdraw(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
		member.withdraw();
	}

}
