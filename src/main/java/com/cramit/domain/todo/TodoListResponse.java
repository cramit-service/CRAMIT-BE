package com.cramit.domain.todo;

import java.time.LocalDateTime;

public record TodoListResponse(
        Long todoId,
        Long weekId,
        String Content,
        String memo,
        LocalDateTime dueDate,
        TodoType todoType,
        Boolean isCompleted,
        Integer sortOrder
) {
}
