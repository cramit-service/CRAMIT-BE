package com.cramit.domain.member.dto;

import com.cramit.domain.subscription.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OnboardingRequest(
		@NotBlank
		@Size(min = 1, max = 255)
		String nickname,

		// 선택 약관만 있는 경우 빈 리스트도 허용하며, 필수 약관 미동의 여부는 서비스 레이어에서 검증한다.
		@NotNull
		List<Long> agreedTermIds,

		@NotNull
		PlanType plan
) {
}
