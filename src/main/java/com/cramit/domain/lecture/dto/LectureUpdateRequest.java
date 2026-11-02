package com.cramit.domain.lecture.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LectureUpdateRequest(
        @NotBlank
        @Size(min = 1, max = 255)
        String title,

        @Size(max = 100)
        String professorName,

        @NotNull
        @Min(1)
        @Max(8)
        Integer colorIndex
) {
}
