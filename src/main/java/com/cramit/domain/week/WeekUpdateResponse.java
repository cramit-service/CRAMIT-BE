package com.cramit.domain.week;

import java.time.LocalDateTime;

public record WeekUpdateResponse(
        Long weekId,
        LocalDateTime weekDate
) {
}
