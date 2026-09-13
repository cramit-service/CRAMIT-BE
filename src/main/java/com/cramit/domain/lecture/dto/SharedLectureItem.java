package com.cramit.domain.lecture.dto;

import com.cramit.domain.lecture.Lecture;
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
    public static SharedLectureItem of(Lecture lecture, Integer weekCount, String ownerNickname) {
        return SharedLectureItem.builder()
                .lectureId(lecture.getLectureId())
                .title(lecture.getTitle())
                .professorName(lecture.getProfessorName())
                .weekCount(weekCount)
                .ownerNickname(ownerNickname)
                .nearestExam(null) // TODO: Exam 도메인 완성되면 계산
                .build();
    }
}
