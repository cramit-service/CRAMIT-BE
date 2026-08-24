package com.cramit.domain.week;

public record WeekUpdateResponse(
        Long weekId,
        Long lecturePptId,
        Long lectureAudioId,
        SttStatus sttStatus
) {
}
