package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiTodoItem;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiTodoClientTest {

    @Test
    @DisplayName("Gemini가 todos 배열을 JSON으로 응답하면 할 일 목록으로 변환하고, 요청에 todos 스키마를 담는다.")
    void generate() {
        // given
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiTodoClient client = new GeminiTodoClient(builder.build());

        String text = "{\\\"todos\\\": [{\\\"weekId\\\": 3, \\\"content\\\": \\\"정렬 정리\\\", \\\"dueDate\\\": \\\"2026-11-10\\\", \\\"memo\\\": \\\"복습\\\"}]}";
        server.expect(requestToUriTemplate("/v1beta/models/{model}:generateContent", GeminiTodoClient.MODEL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.generationConfig.responseSchema.required[0]").value("todos"))
                .andRespond(withSuccess(responseWithText(text), MediaType.APPLICATION_JSON));

        // when
        List<GeminiTodoItem> todos = client.generate("system", "input");

        // then
        assertThat(todos).containsExactly(new GeminiTodoItem(3L, "정렬 정리", "2026-11-10", "복습"));
        server.verify();
    }

    @Test
    @DisplayName("응답 파싱에 실패하면 TODO_GENERATION_FAILED를 던진다.")
    void generateParseFailure() {
        // given
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiTodoClient client = new GeminiTodoClient(builder.build());

        server.expect(requestToUriTemplate("/v1beta/models/{model}:generateContent", GeminiTodoClient.MODEL))
                .andRespond(withSuccess(responseWithText("이건 JSON이 아닙니다"), MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> client.generate("system", "input"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TODO_GENERATION_FAILED));
    }

    private String responseWithText(String text) {
        return """
                {
                  "candidates": [
                    { "content": { "parts": [ {"text": "%s"} ] } }
                  ]
                }
                """.formatted(text);
    }
}
