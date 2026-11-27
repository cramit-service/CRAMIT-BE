package com.cramit.domain.todo.dto;

import java.time.LocalDateTime;

public record TodoCreateResponse(
        Long todoId,
        LocalDateTime createdAt
) {
}
