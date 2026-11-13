package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiRequest;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.http.HttpTimeoutException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(classes = {GeminiClient.class, GeminiClientTest.TestRestClients.class})
@ImportAutoConfiguration({AopAutoConfiguration.class, RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
@ActiveProfiles("test")
class GeminiClientTest {

    private static final GeminiRequest REQUEST = GeminiRequest.of("system", "input", Map.of("type", "OBJECT"));
    private static final String URI = "/v1beta/models/{model}:generateContent";

    @Autowired
    private GeminiClient geminiClient;

    @Autowired
    private MockRestServiceServer defaultServer;

    @Autowired
    private MockRestServiceServer longServer;

    @BeforeEach
    void resetServers() {
        defaultServer.reset();
        longServer.reset();
    }

    @Test
    @DisplayName("타임아웃이 나면 3번까지 시도한 뒤 AI_TIMEOUT을 던진다.")
    void timeoutRetriesThenFails() {
        // given
        defaultServer.expect(ExpectedCount.times(3), requestToUriTemplate(URI, GeminiClient.MODEL))
                .andRespond(withException(new HttpTimeoutException("timeout")));

        // when & then
        assertThatThrownBy(() -> geminiClient.generate(REQUEST, GeminiClient.Timeout.DEFAULT, ErrorCode.CHATBOT_RESPONSE_ERROR))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_TIMEOUT));
        defaultServer.verify();
    }

    @Test
    @DisplayName("요청이 많다는 응답(429)은 다시 시도하지 않고 AI_RATE_LIMITED를 던진다.")
    void rateLimitedFailsWithoutRetry() {
        // given
        defaultServer.expect(ExpectedCount.once(), requestToUriTemplate(URI, GeminiClient.MODEL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        // when & then
        assertThatThrownBy(() -> geminiClient.generate(REQUEST, GeminiClient.Timeout.DEFAULT, ErrorCode.CHATBOT_RESPONSE_ERROR))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_RATE_LIMITED));
        defaultServer.verify();
    }

    @Test
    @DisplayName("그 밖의 오류는 기능별로 넘긴 오류 코드로 바꿔 던진다.")
    void otherErrorUsesFailureCode() {
        // given
        defaultServer.expect(ExpectedCount.once(), requestToUriTemplate(URI, GeminiClient.MODEL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        // when & then
        assertThatThrownBy(() -> geminiClient.generate(REQUEST, GeminiClient.Timeout.DEFAULT, ErrorCode.TODO_GENERATION_FAILED))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TODO_GENERATION_FAILED));
    }

    @Test
    @DisplayName("LONG은 응답 대기 시간이 긴 클라이언트로 보낸다.")
    void longTimeoutUsesLongClient() {
        // given
        longServer.expect(ExpectedCount.once(), requestToUriTemplate(URI, GeminiClient.MODEL))
                .andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));

        // when
        geminiClient.generate(REQUEST, GeminiClient.Timeout.LONG, ErrorCode.SUMMARY_GENERATION_FAILED);

        // then
        longServer.verify();
    }

    @TestConfiguration
    static class TestRestClients {

        private final RestClient.Builder defaultBuilder = RestClient.builder();
        private final RestClient.Builder longBuilder = RestClient.builder();
        private final MockRestServiceServer defaultServer = MockRestServiceServer.bindTo(defaultBuilder).build();
        private final MockRestServiceServer longServer = MockRestServiceServer.bindTo(longBuilder).build();

        @Bean
        RestClient geminiRestClient() {
            return defaultBuilder.build();
        }

        @Bean
        RestClient geminiSummaryRestClient() {
            return longBuilder.build();
        }

        @Bean
        MockRestServiceServer defaultServer() {
            return defaultServer;
        }

        @Bean
        MockRestServiceServer longServer() {
            return longServer;
        }
    }
}
