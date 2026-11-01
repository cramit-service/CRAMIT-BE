package com.cramit.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record StorageProperties(
        String accessKey,
        String secretKey,
        String region,
        S3 s3
) {
    public record S3(
            String bucket,
            String endpoint,
            String publicBaseUrl
    ) {
    }

    public boolean hasCustomEndpoint() {
        return s3.endpoint() != null && !s3.endpoint().isBlank();
    }

    public String publicBaseUrl() {
        if (s3.publicBaseUrl() != null && !s3.publicBaseUrl().isBlank()) {
            return s3.publicBaseUrl();
        }
        return "https://" + s3.bucket() + ".s3." + region + ".amazonaws.com";
    }
}
