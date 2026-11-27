package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.ChatAnswer;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
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
    @DisplayName("Gemini가 JSON으로 응답하면 answer와 referencedPage를 추출한다.")
    void ask() {
        // given
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiChatBotClient client = new GeminiChatBotClient(geminiClient(builder));

        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {"text": "{\\"answer\\": \\"TCP는 연결형 프로토콜입니다.\\", \\"referencedPage\\": 2}"}
                        ]
                      }
                    }
                  ]
                }
                """;

        server.expect(requestToUriTemplate("/v1beta/models/{model}:generateContent", GeminiClient.MODEL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // when
        ChatAnswer answer = client.ask("TCP와 UDP 차이를 알려줘.", "[p.2] 네트워크 강의 요약본...");

        // then
        assertThat(answer.answer()).isEqualTo("TCP는 연결형 프로토콜입니다.");
        assertThat(answer.referencedPage()).isEqualTo(2);
        server.verify();
    }

    @Test
    @DisplayName("응답 파싱에 실패하면 CHATBOT_RESPONSE_ERROR를 던진다.")
    void askParseFailure() {
        // given
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiChatBotClient client = new GeminiChatBotClient(geminiClient(builder));

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

        server.expect(requestToUriTemplate("/v1beta/models/{model}:generateContent", GeminiClient.MODEL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(malformedBody, MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> client.ask("질문", "context"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_RESPONSE_ERROR));
    }

    private GeminiClient geminiClient(RestClient.Builder builder) {
        RestClient restClient = builder.build();
        return new GeminiClient(restClient, restClient);
    }
}
