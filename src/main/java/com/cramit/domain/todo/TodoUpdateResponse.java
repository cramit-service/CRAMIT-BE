package com.cramit.domain.todo;

public record TodoUpdateResponse(
        Long todoId,
        Long weekId
) {
}
