package com.cramit.domain.ai.dto;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String extractAnswer() {
        AnswerPayload payload;
        try {
            String rawJson = candidates.get(0).content().parts().get(0).text();
            payload = OBJECT_MAPPER.readValue(rawJson, AnswerPayload.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CHATBOT_RESPONSE_ERROR);
        }

        String answer = payload.answer();
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(ErrorCode.CHATBOT_RESPONSE_ERROR);
        }
        return answer;
    }

    public record Candidate(Content content) {}

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    private record AnswerPayload(String answer) {}
}
