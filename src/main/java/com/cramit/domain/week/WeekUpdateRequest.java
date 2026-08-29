package com.cramit.domain.week;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record WeekUpdateRequest(
        @NotBlank
        @Size(min = 1, max = 255)
        String title,

        @NotNull
        LocalDateTime weekDate,

        @Size(max = 100)
        String professorName
) {
}
