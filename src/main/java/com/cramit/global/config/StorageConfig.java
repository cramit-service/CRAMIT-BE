package com.cramit.global.config;

import com.cramit.global.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final StorageProperties storageProperties;

    // 업로드가 끝났는지(파일이 실제로 있는지와 크기) 확인할 때 쓴다.
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(storageProperties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(storageProperties.accessKey(), storageProperties.secretKey())));

        if (storageProperties.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(storageProperties.s3().endpoint()))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(storageProperties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(storageProperties.accessKey(), storageProperties.secretKey())));

        // R2 등 S3 호환 저장소는 버킷별 서브도메인이 아니라 엔드포인트 경로로 버킷을 지정한다
        if (storageProperties.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(storageProperties.s3().endpoint()))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }
}
