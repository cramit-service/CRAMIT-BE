package com.cramit.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
        @NotBlank(message = "질문을 입력해주세요.")
        String message
) {
}
