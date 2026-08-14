package com.cramit.domain.lecture;

public record LectureDetailResponse(
        Long lectureId,
        String title,
        String professorName,
        Boolean isOwner,
        String ownerNickname,
        Integer memberCount,
        String createdAt
) {
}
