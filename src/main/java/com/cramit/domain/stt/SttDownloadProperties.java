package com.cramit.domain.stt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stt.download")
public record SttDownloadProperties(
        String allowedUrlPrefix
) {
}
