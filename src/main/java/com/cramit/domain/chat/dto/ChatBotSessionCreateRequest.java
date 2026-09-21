package com.cramit.domain.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatBotSessionCreateRequest(
        @NotNull
        Long weekId,

        @Size(min = 1, max = 255)
        String title
) {
}
