package com.cramit.domain.stt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stt.azure")
public record AzureSpeechProperties(
        String endpoint,
        String apiKey
) {
}
