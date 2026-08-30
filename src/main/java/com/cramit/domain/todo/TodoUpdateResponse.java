package com.cramit.domain.todo;

public record TodoUpdateResponse(
        Long TodoId,
        Long weekId
) {
}
