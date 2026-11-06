package com.cramit.domain.ai.dto;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

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

    public String extractText(ErrorCode errorCode) {
        try {
            String text = candidates.get(0).content().parts().get(0).text();
            if (text == null || text.isBlank()) {
                throw new BusinessException(errorCode);
            }
            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(errorCode);
        }
    }

    public record Candidate(Content content) {}

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    private record AnswerPayload(String answer) {}
}
