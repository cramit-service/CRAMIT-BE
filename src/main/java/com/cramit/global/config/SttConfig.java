package com.cramit.global.config;

import com.cramit.domain.stt.GroqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class SttConfig {

    private final GroqProperties groqProperties;

    // 50분 녹음도 한 번에 전사하므로 응답 대기를 길게 둔다.
    @Bean
    public RestClient groqRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .requestFactory(requestFactory(Duration.ofMinutes(5)))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + groqProperties.apiKey())
                .build();
    }

    @Bean
    public RestClient audioDownloadRestClient() {
        return RestClient.builder()
                .requestFactory(requestFactory(Duration.ofMinutes(2)))
                .build();
    }

    private JdkClientHttpRequestFactory requestFactory(Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return requestFactory;
    }
}
