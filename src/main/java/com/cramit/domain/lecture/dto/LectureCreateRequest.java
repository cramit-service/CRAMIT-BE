package com.cramit.domain.lecture.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LectureCreateRequest(

        @NotBlank
        @Size(min = 1, max = 255)
        String title,

        @Size(max = 100)
        String professorName
) {
}
