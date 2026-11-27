package com.cramit.domain.member.dto;

public record TokenResponse(
		String accessToken,
		String refreshToken
) {
}
