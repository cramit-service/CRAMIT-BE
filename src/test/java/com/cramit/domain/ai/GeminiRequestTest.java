package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.ChatAnswer;
import com.cramit.domain.ai.dto.GeminiRequest;
import com.cramit.domain.ai.dto.GeminiResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiRequestTest {

    private static final Map<String, Object> CHAT_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "answer", Map.of("type", "STRING"),
                    "referencedPage", Map.of("type", "INTEGER")
            ),
            "required", List.of("answer")
    );

    @Test
    @DisplayName("요청에 전달한 responseSchema가 그대로 담긴다")
    void containsResponseSchema() {
        //given
        GeminiRequest request = GeminiRequest.of("system", "userText", CHAT_SCHEMA);

        //when, then
        assertThat(request.generationConfig().responseSchema()).isEqualTo(CHAT_SCHEMA);
        assertThat(request.generationConfig().responseMimeType()).isEqualTo("application/json");
    }

    @Test
    @DisplayName("answer와 referencedPage가 있으면 그대로 반환한다.")
    void extractChatAnswer() {
        // given
        GeminiResponse response = responseOf("{\"answer\": \"TCP는 연결형입니다.\", \"referencedPage\": 3}");

        // when
        ChatAnswer answer = response.extractChatAnswer();

        // then
        assertThat(answer.answer()).isEqualTo("TCP는 연결형입니다.");
        assertThat(answer.referencedPage()).isEqualTo(3);
    }

    @Test
    @DisplayName("referencedPage가 없으면 null로 반환한다.")
    void extractChatAnswerWithoutReferencedPage() {
        // given
        GeminiResponse response = responseOf("{\"answer\": \"TCP는 연결형입니다.\"}");

        // when
        ChatAnswer answer = response.extractChatAnswer();

        // then
        assertThat(answer.referencedPage()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"answer\": null}",
            "{\"answer\": \"\"}",
            "{\"answer\": \"   \"}"
    })
    @DisplayName("answer가 null이거나 비어 있으면 CHATBOT_RESPONSE_ERROR를 던진다.")
    void extractChatAnswerBlank(String rawJson) {
        // given
        GeminiResponse response = responseOf(rawJson);

        // when & then
        assertThatThrownBy(response::extractChatAnswer)
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_RESPONSE_ERROR));
    }

    @Test
    @DisplayName("candidates가 비어 있으면 CHATBOT_RESPONSE_ERROR를 던진다.")
    void extractChatAnswerEmptyCandidates() {
        // given
        GeminiResponse response = new GeminiResponse(List.of());

        // when & then
        assertThatThrownBy(response::extractChatAnswer)
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_RESPONSE_ERROR));
    }

    private GeminiResponse responseOf(String text) {
        return new GeminiResponse(List.of(
                new GeminiResponse.Candidate(
                        new GeminiResponse.Content(List.of(new GeminiResponse.Part(text))))));
    }
}
