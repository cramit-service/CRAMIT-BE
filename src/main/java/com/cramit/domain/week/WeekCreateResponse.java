package com.cramit.domain.week;

import java.time.LocalDateTime;

public record WeekCreateResponse(
        Long weekId,
        Long lecturePptId,
        Long lectureAudioId,
        SttStatus sttStatus,
        LocalDateTime createdAt
) {
}
