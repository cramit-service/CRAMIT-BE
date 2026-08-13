package com.cramit.domain.lecture;

import java.time.LocalDateTime;

public record LectureCreateResponse(
        Long lectureId,
        LocalDateTime createdAt
) {
}
