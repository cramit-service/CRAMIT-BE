package com.cramit.domain.week.dto;

import com.cramit.domain.week.enums.WeekStatus;
import jakarta.validation.constraints.NotNull;

public record WeekStatusUpdateRequest(
        @NotNull
        WeekStatus status
) {
}
