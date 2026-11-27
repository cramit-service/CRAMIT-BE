package com.cramit.domain.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExamCreateRequest(
        @NotBlank
        @Size(max = 10)
        String title,

        @NotNull
        LocalDate examDate,

        @Size(max = 255)
        String memo
) {
}
