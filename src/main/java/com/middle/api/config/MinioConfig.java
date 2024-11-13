package com.middle.api.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.access-key}")
    private String minioAccessKey;

    @Value("${minio.secret-key}")
    private String minioSecretKey;

    @Bean
    public MinioClient minioClient() {

        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(minioAccessKey, minioSecretKey)
                .build();

        try {
            // Create videos bucket if it doesn't exist
            String videosBucket = "videos-bucket";
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(videosBucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(videosBucket).build());
            }

            // Create images bucket if it doesn't exist
            String imagesBucket = "images-bucket";
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(imagesBucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(imagesBucket).build());
            }

            // Create test bucket if it doesn't exist
            String testBucket = "test-bucket";
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(testBucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(testBucket).build());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error initializing MinIO buckets", e);
        }

        return minioClient;
    }

}

