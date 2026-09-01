package com.cramit.domain.lecture.dto;

import com.cramit.domain.lecture.Lecture;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LectureDetailResponse(
        Long lectureId,
        String title,
        String professorName,
        Boolean isOwner,
        String ownerNickname,
        Integer memberCount,
        LocalDateTime createdAt
) {
    public static LectureDetailResponse from(
            Lecture lecture, Long memberId, String ownerNickname, Integer memberCount
    ) {
        return LectureDetailResponse.builder()
                .lectureId(lecture.getLectureId())
                .title(lecture.getTitle())
                .professorName(lecture.getProfessorName())
                .isOwner(lecture.isOwnedBy(memberId))
                .ownerNickname(ownerNickname)
                .memberCount(memberCount)
                .createdAt(lecture.getCreatedAt())
                .build();
    }
}
