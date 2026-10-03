package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiRequest;
import com.cramit.domain.ai.dto.GeminiResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiRequestTest {
    @Test
    @DisplayName("요청에 answer 필수 스키마가 포함된다")
    void containsAnswerSchema() {
        //given
        GeminiRequest request = GeminiRequest.of("system", "question", "context");

        //when
        JsonNode json = new ObjectMapper().valueToTree(request);
        JsonNode schema = json.path("generationConfig").path("responseSchema");

        //then
        assertThat(schema.path("type").asText()).isEqualTo("OBJECT");
        assertThat(schema.path("properties").path("answer").path("type").asText()).isEqualTo("STRING");
        assertThat(schema.path("required").get(0).asText()).isEqualTo("answer");
    }

    @Test
    @DisplayName("answer가 있으면 그대로 반환한다.")
    void extractAnswer() {
        // given
        GeminiResponse response = responseOf("{\"answer\": \"TCP는 연결형입니다.\"}");

        // when
        String answer = response.extractAnswer();

        // then
        assertThat(answer).isEqualTo("TCP는 연결형입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"answer\": null}",
            "{\"answer\": \"\"}",
            "{\"answer\": \"   \"}"
    })
    @DisplayName("answer가 null이거나 비어 있으면 CHATBOT_RESPONSE_ERROR를 던진다.")
    void extractAnswerBlank(String rawJson) {
        // given
        GeminiResponse response = responseOf(rawJson);

        // when & then
        assertThatThrownBy(response::extractAnswer)
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_RESPONSE_ERROR));
    }

    @Test
    @DisplayName("candidates가 비어 있으면 CHATBOT_RESPONSE_ERROR를 던진다.")
    void extractAnswerEmptyCandidates() {
        // given
        GeminiResponse response = new GeminiResponse(List.of());

        // when & then
        assertThatThrownBy(response::extractAnswer)
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_RESPONSE_ERROR));
    }

    private GeminiResponse responseOf(String text) {
        return new GeminiResponse(List.of(
                new GeminiResponse.Candidate(
                        new GeminiResponse.Content(List.of(new GeminiResponse.Part(text))))));
    }
}
