package com.cramit.domain.ai;

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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiSummaryClientTest {

    @Test
    @DisplayName("Gemini가 JSON으로 응답하면 summary를 추출하고, 요청에 summary 스키마를 담는다.")
    void summarize() {
        // given
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiSummaryClient client = new GeminiSummaryClient(builder.build());

        server.expect(requestToUriTemplate("/v1beta/models/{model}:generateContent", GeminiSummaryClient.MODEL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.generationConfig.responseSchema.required[0]").value("summary"))
                .andRespond(withSuccess(responseWithText("{\\\"summary\\\": \\\"## 정렬 (p.3)\\\"}"), MediaType.APPLICATION_JSON));

        // when
        String summary = client.summarize("system", "[p.3] 정렬 설명");

        // then
        assertThat(summary).isEqualTo("## 정렬 (p.3)");
        server.verify();
    }

    @Test
    @DisplayName("응답 파싱에 실패하면 AI_RESPONSE_PARSE_ERROR를 던진다.")
    void summarizeParseFailure() {
        // given
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiSummaryClient client = new GeminiSummaryClient(builder.build());

        server.expect(requestToUriTemplate("/v1beta/models/{model}:generateContent", GeminiSummaryClient.MODEL))
                .andRespond(withSuccess(responseWithText("이건 JSON이 아닙니다"), MediaType.APPLICATION_JSON));

        // when & then
        assertThatThrownBy(() -> client.summarize("system", "스크립트"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_PARSE_ERROR));
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
