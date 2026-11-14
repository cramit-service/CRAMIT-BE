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

class GeminiEmbeddingClientTest {

    @Test
    @DisplayName("문서 임베딩 요청 시 taskType=RETRIEVAL_DOCUMENT와 title을 담고, 응답 values를 float[]로 변환한다.")
    void embedDocument() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiEmbeddingClient client = new GeminiEmbeddingClient(builder.build());

        server.expect(requestToUriTemplate("/v1beta/models/{model}:embedContent", GeminiEmbeddingClient.MODEL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.taskType").value("RETRIEVAL_DOCUMENT"))
                .andExpect(jsonPath("$.title").value("1주차"))
                .andExpect(jsonPath("$.content.parts[0].text").value("재귀는 함수가 자기 자신을 호출하는 것입니다."))
                .andRespond(withSuccess(embeddingResponse(0.1, 0.2, 0.3), MediaType.APPLICATION_JSON));

        float[] embedding = client.embedDocument("1주차", "재귀는 함수가 자기 자신을 호출하는 것입니다.");

        assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f);
        server.verify();
    }

    @Test
    @DisplayName("질의 임베딩 요청 시 taskType=RETRIEVAL_QUERY를 담는다.")
    void embedQuery() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiEmbeddingClient client = new GeminiEmbeddingClient(builder.build());

        server.expect(requestToUriTemplate("/v1beta/models/{model}:embedContent", GeminiEmbeddingClient.MODEL))
                .andExpect(jsonPath("$.taskType").value("RETRIEVAL_QUERY"))
                .andRespond(withSuccess(embeddingResponse(0.5, 0.6), MediaType.APPLICATION_JSON));

        float[] embedding = client.embedQuery("재귀가 뭐야?");

        assertThat(embedding).containsExactly(0.5f, 0.6f);
        server.verify();
    }

    @Test
    @DisplayName("응답에 embedding 값이 없으면 EMBEDDING_GENERATION_FAILED를 던진다.")
    void embedDocumentEmptyResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiEmbeddingClient client = new GeminiEmbeddingClient(builder.build());

        server.expect(requestToUriTemplate("/v1beta/models/{model}:embedContent", GeminiEmbeddingClient.MODEL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embedDocument("제목", "내용"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMBEDDING_GENERATION_FAILED));
    }

    private String embeddingResponse(double... values) {
        StringBuilder valuesJson = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                valuesJson.append(",");
            }
            valuesJson.append(values[i]);
        }
        return "{\"embedding\": {\"values\": [" + valuesJson + "]}}";
    }
}
