package com.cramit.domain.chat.dto;

import com.cramit.domain.chat.enums.SenderType;
import com.cramit.domain.chat.entity.ChatMessage;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatMessageResponse(
        Long chatMessageId,
        SenderType senderType,
        String message,
        Integer referencedPage,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .chatMessageId(chatMessage.getChatMessageId())
                .senderType(chatMessage.getSenderType())
                .message(chatMessage.getMessage())
                .referencedPage(chatMessage.getReferencedPage())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
