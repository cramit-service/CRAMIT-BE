package com.cramit.domain.todo.dto;

import java.time.LocalDateTime;

public record TodoToggleResponse(
        Long todoId,
        Boolean isCompleted,
        LocalDateTime completedAt
) {
}
