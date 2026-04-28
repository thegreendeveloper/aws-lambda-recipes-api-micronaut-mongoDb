package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.generated.model.CreateRecipeRequest;
import com.recipes.api.service.RecipesService;
import io.micronaut.function.aws.MicronautRequestHandler;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.util.List;

public class ImportRecipesHandler extends MicronautRequestHandler<S3Event, Void> {

    @Inject
    private RecipesService service;

    @Inject
    private S3Client s3Client;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public Void execute(S3Event event) {
        event.getRecords().forEach(this::processRecord);
        return null;
    }

    private void processRecord(S3EventNotificationRecord record) {
        String bucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();
        parseRecipes(downloadFileContent(bucket, key)).forEach(this::importRecipe);
    }

    private String downloadFileContent(String bucket, String key) {
        return s3Client.getObjectAsBytes(buildGetObjectRequest(bucket, key)).asUtf8String();
    }

    private GetObjectRequest buildGetObjectRequest(String bucket, String key) {
        return GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
    }

    private List<CreateRecipeRequest> parseRecipes(String content) {
        try {
            return objectMapper.readValue(content, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse recipes JSON", e);
        }
    }

    private void importRecipe(CreateRecipeRequest recipe) {
        service.createRecipe(
                recipe.getName(),
                recipe.getCuisine(),
                recipe.getPrepTimeMinutes(),
                recipe.getIngredients(),
                recipe.getSteps());
    }
}
