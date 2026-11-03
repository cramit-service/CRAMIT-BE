package com.cramit.domain.week.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WeekReviewCountUpdateRequest(
        @NotNull
        @Min(0)
        Integer reviewCount
) {
}
