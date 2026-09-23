package com.cramit.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class GeminiConfig {

    private final GeminiProperties geminiProperties;

    @Bean
    public RestClient geminiRestClient() {
        return RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().add("x-goog-api-key", geminiProperties.apiKey());
                    return execution.execute(request, body);
                })
                .build();
    }
}
