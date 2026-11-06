package com.cramit.domain.todo.dto;

import jakarta.validation.constraints.NotNull;

public record AiTodoGenerateRequest(
        @NotNull
        Long examId
) {
}
