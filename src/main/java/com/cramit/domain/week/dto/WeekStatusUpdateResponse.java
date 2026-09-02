package com.cramit.domain.week.dto;

import com.cramit.domain.week.enums.WeekStatus;

public record WeekStatusUpdateResponse(
        Long weekId,
        WeekStatus status
) {
}
