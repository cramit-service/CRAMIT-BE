package com.cramit.domain.chat.dto;

import com.cramit.domain.chat.SenderType;
import com.cramit.domain.chat.entity.ChatBot;
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
    public static ChatMessageResponse from(ChatBot chatBot) {
        return ChatMessageResponse.builder()
                .chatMessageId(chatBot.getChatMessageId())
                .senderType(chatBot.getSenderType())
                .message(chatBot.getMessage())
                .referencedPage(chatBot.getReferencedPage())
                .createdAt(chatBot.getCreatedAt())
                .build();
    }
}
