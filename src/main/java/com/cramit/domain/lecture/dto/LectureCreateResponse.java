package com.cramit.domain.lecture.dto;

import java.time.LocalDateTime;

public record LectureCreateResponse(
        Long lectureId,
        LocalDateTime createdAt
) {
}
