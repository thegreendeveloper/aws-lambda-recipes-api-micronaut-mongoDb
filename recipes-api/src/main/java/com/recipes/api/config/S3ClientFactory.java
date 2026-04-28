package com.recipes.api.config;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

@Factory
public class S3ClientFactory {

    @Singleton
    public S3Client s3Client() {
        return S3Client.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }
}
