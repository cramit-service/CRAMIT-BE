package com.cramit.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

public record ChatBotSessionCreateRequest(
        @NotNull
        Long weekId,

        String title
) {
}
