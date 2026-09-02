package com.cramit.domain.week.dto;

import java.time.LocalDateTime;

public record WeekUpdateResponse(
        Long weekId,
        LocalDateTime weekDate
) {
}
