package com.cramit.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatBotSessionCreateResponse(
        Long chatBotSessionId,
        LocalDateTime createdAt
) {
}
