package com.cramit.domain.lecture;

import java.time.LocalDateTime;

public record LectureDetailResponse(
        Long lectureId,
        String title,
        String professorName,
        Boolean isOwner,
        String ownerNickname,
        Integer memberCount,
        LocalDateTime createdAt
) {
}
