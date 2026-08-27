package com.cramit.domain.member.dto;

import java.time.LocalDateTime;

public record ProfileResponse(
		Long memberId,
		String nickname,
		String profileImageUrl,
		String socialProvider,
		LocalDateTime createdAt
) {
}
