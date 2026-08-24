package com.cramit.domain.week;

import java.time.LocalDateTime;

public record WeekListResponse(
        Long weekId,
        String title,
        LocalDateTime weekDate,
        String professorName,
        WeekStatus status
) {
}
