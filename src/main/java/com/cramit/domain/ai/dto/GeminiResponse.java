package com.cramit.domain.ai.dto;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String extractAnswer() {
        String rawJson = candidates.get(0).content().parts().get(0).text();
        try {
            AnswerPayload payload = OBJECT_MAPPER.readValue(rawJson, AnswerPayload.class);
            return payload.answer();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CHATBOT_RESPONSE_ERROR);
        }
    }

    public record Candidate(Content content) {}

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    private record AnswerPayload(String answer) {}
}
