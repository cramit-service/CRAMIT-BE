package com.cramit.domain.todo.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record TodoCreateRequest(
        Long weekId,

        @NotBlank
        String content,

        LocalDateTime dueDate,

        String memo
) {
}
