package com.cramit.domain.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// 수정 모달이 강의 선택까지 통째로 저장하므로 다른 강의로 옮길 수 있다.
public record ExamUpdateRequest(
        @NotNull
        Long lectureId,

        @NotBlank
        @Size(max = 10)
        String title,

        @NotNull
        LocalDate examDate,

        @Size(max = 255)
        String memo
) {
}
