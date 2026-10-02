package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
