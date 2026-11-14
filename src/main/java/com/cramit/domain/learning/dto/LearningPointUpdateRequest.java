package com.cramit.domain.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LearningPointUpdateRequest(
        @NotBlank
        @Size(max = 255)
        String title
) {
}
