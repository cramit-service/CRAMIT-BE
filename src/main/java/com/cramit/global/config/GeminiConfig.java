package com.cramit.global.config;

import com.cramit.domain.ai.GeminiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class GeminiConfig {

    private final GeminiProperties geminiProperties;

    @Bean
    public RestClient geminiRestClient() {
        return buildGeminiRestClient(Duration.ofSeconds(20));
    }

    @Bean
    public RestClient geminiSummaryRestClient() {
        return buildGeminiRestClient(Duration.ofSeconds(90));
    }

    private RestClient buildGeminiRestClient(Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().add("x-goog-api-key", geminiProperties.apiKey());
                    return execution.execute(request, body);
                })
                .build();
    }
}
