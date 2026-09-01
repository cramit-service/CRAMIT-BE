package com.cramit.domain.lecture.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record LectureUpdateRequest(
        @NotBlank
        @Size(min = 1, max = 255)
        String title,

        @Size(max = 100)
        String professorName,

        LocalDateTime examDate

) {
}
