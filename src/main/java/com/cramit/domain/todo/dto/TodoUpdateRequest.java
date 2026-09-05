package com.cramit.domain.todo.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record TodoUpdateRequest(
        Long weekId,

        @NotBlank
        String content,

        String memo,

        LocalDateTime dueDate
) {
}
