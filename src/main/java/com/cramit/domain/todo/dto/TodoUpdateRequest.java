package com.cramit.domain.todo.dto;

import jakarta.validation.constraints.NotBlank;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDateTime;

// weekId/memo/dueDate는 필드 생략 시 기존 값 유지, null 명시 시 값을 지운다. content는 항상 필요하다.
public record TodoUpdateRequest(
        JsonNullable<Long> weekId,

        @NotBlank
        String content,

        JsonNullable<String> memo,

        JsonNullable<LocalDateTime> dueDate
) {
}
