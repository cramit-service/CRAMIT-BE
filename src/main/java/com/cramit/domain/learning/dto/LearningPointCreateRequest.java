package com.cramit.domain.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LearningPointCreateRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        String selectedText,

        @NotNull
        @PositiveOrZero
        Integer markdownPosition,

        @PositiveOrZero
        Integer audioTimestamp
) {
}
