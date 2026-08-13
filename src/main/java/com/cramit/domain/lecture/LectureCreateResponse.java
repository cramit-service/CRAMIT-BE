package com.cramit.domain.lecture;

import java.time.LocalTime;

public record LectureCreateResponse(
        Long lectureId,
        LocalTime createdAt
) {
}
