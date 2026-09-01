package com.cramit.domain.todo;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TodoListResponse(
        Long todoId,
        Long weekId,
        String content,
        String memo,
        LocalDateTime dueDate,
        TodoType todoType,
        Boolean isCompleted,
        Integer sortOrder
) {
    public static TodoListResponse from(Todo todo) {
        return TodoListResponse.builder()
                .todoId(todo.getTodoId())
                .weekId(todo.getWeekId())
                .content(todo.getContent())
                .memo(todo.getMemo())
                .dueDate(todo.getDueDate())
                .todoType(todo.getTodoType())
                .isCompleted(todo.isCompleted())
                .sortOrder(todo.getSortOrder())
                .build();
    }
}
