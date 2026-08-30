package com.cramit.domain.todo;

import java.time.LocalDateTime;

public record TodoCreateResponse(
        Long todoId,
        LocalDateTime createdAt
) {
}
