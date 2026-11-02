package com.cramit.domain.stt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stt.groq")
public record GroqProperties(
        String apiKey
) {
}
