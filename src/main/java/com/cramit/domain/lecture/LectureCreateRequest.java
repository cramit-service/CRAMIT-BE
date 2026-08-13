package com.cramit.domain.lecture;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LectureCreateRequest(

        @NotBlank
        @Size(max = 100)
        String title,

        @Size(max = 100)
        String professorName
) {
}
