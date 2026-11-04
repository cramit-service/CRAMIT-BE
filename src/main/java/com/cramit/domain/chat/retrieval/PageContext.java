package com.cramit.domain.chat.retrieval;

public record PageContext(
        Integer pageNumber,
        String title,
        String content
) {
}
