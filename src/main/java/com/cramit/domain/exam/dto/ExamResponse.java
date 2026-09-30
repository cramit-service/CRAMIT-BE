package com.cramit.domain.exam.dto;

import com.cramit.domain.exam.entity.Exam;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record ExamResponse(
        Long examId,
        Long lectureId,
        String lectureName,
        String title,
        LocalDate examDate,
        String memo,
        int progress,
        LocalDateTime createdAt
) {
    public static ExamResponse of(Exam exam, String lectureName, int progress) {
        return ExamResponse.builder()
                .examId(exam.getExamId())
                .lectureId(exam.getLectureId())
                .lectureName(lectureName)
                .title(exam.getTitle())
                .examDate(exam.getExamDate())
                .memo(exam.getMemo())
                .progress(progress)
                .createdAt(exam.getCreatedAt())
                .build();
    }
}
