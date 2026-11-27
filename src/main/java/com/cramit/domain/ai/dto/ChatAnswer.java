package com.cramit.domain.ai.dto;

public record ChatAnswer(
        String answer,
        Integer referencedPage
) {
}
