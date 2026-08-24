package com.cramit.domain.week;

public record WeekStatusUpdateResponse(
        Long weekId,
        WeekStatus status
) {
}
