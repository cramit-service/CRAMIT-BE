package com.cramit.domain.ai.dto;

public record GeminiTodoItem(
        Long weekId,
        String content,
        String dueDate,
        String memo
) {
}
