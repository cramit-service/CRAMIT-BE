package com.cramit.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
		@NotBlank
		@Size(min = 1, max = 255)
		String nickname,

		String profileImageUrl
) {
}
