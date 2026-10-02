package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiRequest;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiChatBotClientTest {

    @Test
    @DisplayName("Gemini가 JSON으로 응답하면 answer를 추출한다.")
    void ask() {
        // given
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiChatBotClient client = new GeminiChatBotClient(builder.build());

        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {"text": "{\\"answer\\": \\"TCP는 연결형 프로토콜입니다.\\"}"}
                        ]
                      }
                    }
                  ]
                }
                """;

        server.expect(requestToUriTemplate("/v1beta/models/{model}:generateContent", GeminiChatBotClient.MODEL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // when
        String answer = client.ask("TCP와 UDP 차이를 알려줘.", "네트워크 강의 요약본...");

        // then
        assertThat(answer).isEqualTo("TCP는 연결형 프로토콜입니다.");
        server.verify();
    }

    @Test
    @DisplayName("응답 파싱에 실패하면 CHATBOT_RESPONSE_ERROR를 던진다.")
    void askParseFailure() {
        // given
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiChatBotClient client = new GeminiChatBotClient(builder.build());

        String malformedBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {"text": "이건 JSON이 아닙니다"}
                        ]
                      }
                    }
                  ]
                }
                """;

        server.expect(requestToUriTemplate("/v1beta/models/{model}:generateContent", GeminiChatBotClient.MODEL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(malformedBody, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> client.ask("질문", "context"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_RESPONSE_ERROR));
    }

    @Test
    @DisplayName("요청에 answer 필수 스키마가 포함된다")
    void answer() {
        GeminiRequest request = GeminiRequest.of("system", "question", "context");

        JsonNode json = new ObjectMapper().valueToTree(request);
        JsonNode schema = json.path("generationConfig").path("responseSchema");

        assertThat(schema.path("type").asText()).isEqualTo("OBJECT");
        assertThat(schema.path("properties").path("answer").path("type").asText()).isEqualTo("STRING");
        assertThat(schema.path("required").get(0).asText()).isEqualTo("answer");
    }

}
