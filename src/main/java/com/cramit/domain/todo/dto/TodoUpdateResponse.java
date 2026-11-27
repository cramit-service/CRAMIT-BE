package com.cramit.domain.todo.dto;

public record TodoUpdateResponse(
        Long todoId,
        Long weekId
) {
}
