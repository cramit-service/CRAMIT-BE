package com.cramit.domain.lecture.dto;

import com.cramit.domain.lecture.Lecture;
import lombok.Builder;

@Builder
public record MyLectureItem(
        Long lectureId,
        String title,
        String professorName,
        Integer weekCount,
        NearestExam nearestExam
) {
    public static MyLectureItem from(Lecture lecture, Integer weekCount) {
        return MyLectureItem.builder()
                .lectureId(lecture.getLectureId())
                .title(lecture.getTitle())
                .professorName(lecture.getProfessorName())
                .weekCount(weekCount)
                .nearestExam(null) // TODO: Exam 완성되면 계산 로직 추가
                .build();
    }
}