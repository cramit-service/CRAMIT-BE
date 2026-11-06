package com.cramit.domain.member.dto;

public record OnboardingResponse(
		Long memberId,
		String nickname,
		String plan
) {
}
