package com.cramit.domain.week.dto;

import java.time.LocalDate;

public record WeekUpdateResponse(
        Long weekId,
        LocalDate weekDate
) {
}
