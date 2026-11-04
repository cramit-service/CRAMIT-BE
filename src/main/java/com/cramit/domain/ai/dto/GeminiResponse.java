package com.cramit.domain.ai.dto;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ChatAnswer extractChatAnswer() {
        ChatAnswerPayload payload;
        try {
            String rawJson = candidates.get(0).content().parts().get(0).text();
            payload = OBJECT_MAPPER.readValue(rawJson, ChatAnswerPayload.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CHATBOT_RESPONSE_ERROR);
        }

        if (payload.answer() == null || payload.answer().isBlank()) {
            throw new BusinessException(ErrorCode.CHATBOT_RESPONSE_ERROR);
        }
        return new ChatAnswer(payload.answer(), payload.referencedPage());
    }

    public String extractField(String fieldName, ErrorCode errorCode) {
        JsonNode value;
        try {
            String rawJson = candidates.get(0).content().parts().get(0).text();
            value = OBJECT_MAPPER.readTree(rawJson).get(fieldName);
        } catch (Exception e) {
            throw new BusinessException(errorCode);
        }

        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new BusinessException(errorCode);
        }
        return value.asText();
    }

    public record Candidate(Content content) {}

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    private record ChatAnswerPayload(String answer, Integer referencedPage) {}
}
