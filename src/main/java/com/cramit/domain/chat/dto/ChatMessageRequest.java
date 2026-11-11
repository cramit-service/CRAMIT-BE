package com.cramit.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "질문을 입력해주세요.")
        String message,

        @Size(max = 2000, message = "선택한 부분은 2000자 이하로 입력해주세요.")
        String selectedText
) {
        public ChatMessageRequest(String message) {
                this(message, null);
        }

        public boolean hasSelectedText() {
                return selectedText != null && !selectedText.isBlank();
        }
}
