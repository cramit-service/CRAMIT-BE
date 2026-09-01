package com.cramit.domain.lecture.dto;

import lombok.Builder;

@Builder
public record SharedLectureItem(
        Long lectureId,
        String title,
        String professorName,
        Integer weekCount,
        String ownerNickname,
        NearestExam nearestExam
) {
}
