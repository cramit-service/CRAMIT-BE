package com.cramit.domain.week;

import jakarta.validation.constraints.NotNull;

public record WeekStatusUpdateRequest(
        @NotNull
        WeekStatus status
) {
}
