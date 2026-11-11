package com.cramit.domain.lecture.dto;

import com.cramit.domain.lecture.Lecture;
import lombok.Builder;

@Builder
public record MyLectureItem(
        Long lectureId,
        String title,
        String professorName,
        Integer weekCount,
        Integer colorIndex
) {
    public static MyLectureItem from(Lecture lecture, int weekCount) {
        return MyLectureItem.builder()
                .lectureId(lecture.getLectureId())
                .title(lecture.getTitle())
                .professorName(lecture.getProfessorName())
                .weekCount(weekCount)
                .colorIndex(lecture.getColorIndex())
                .build();
    }
}