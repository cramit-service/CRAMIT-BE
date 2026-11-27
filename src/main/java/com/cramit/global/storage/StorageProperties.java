package com.cramit.global.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aws")
public record StorageProperties(
        String accessKey,
        String secretKey,
        String region,
        @Valid @NotNull S3 s3
) {
    public record S3(
            String bucket,
            String endpoint,
            @NotBlank String publicBaseUrl
    ) {
    }

    public boolean hasCustomEndpoint() {
        return s3.endpoint() != null && !s3.endpoint().isBlank();
    }
}
