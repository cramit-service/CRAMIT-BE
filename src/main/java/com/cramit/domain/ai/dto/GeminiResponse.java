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
        return new ChatAnswer(payload.answer(), toIntegralPage(payload.referencedPage()));
    }

    // Integer로 바로 바인딩하면 Jackson 기본 설정상 2.7 같은 소수도 2로 잘려 들어온다.
    // 정수 노드가 아니면 페이지 번호를 신뢰하지 않고 답변만 저장한다.
    private Integer toIntegralPage(JsonNode node) {
        return node != null && node.isInt() ? node.intValue() : null;
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

    private record ChatAnswerPayload(String answer, JsonNode referencedPage) {}
}
