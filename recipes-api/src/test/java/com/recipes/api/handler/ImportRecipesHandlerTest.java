package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.service.RecipesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportRecipesHandlerTest {

    @Mock
    private RecipesService service;

    @Mock
    private S3Client s3Client;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private ImportRecipesHandler handler;

    @Test
    void executeCallsServiceForEachRecipeInFile() {
        stubS3Download(twoRecipesJson());

        handler.execute(buildEvent("my-bucket", "recipes.json"));

        verify(service, times(2)).createRecipe(any(), any(), any(Integer.class), any(), any());
    }

    @Test
    void executePassesCorrectFieldsToService() {
        stubS3Download(singleRecipeJson());

        handler.execute(buildEvent("bucket", "file.json"));

        verify(service).createRecipe("Pasta", "Italian", 20, List.of("pasta"), List.of("boil"));
    }

    @Test
    void executeThrowsOnInvalidJson() {
        stubS3Download("not-json{{");

        assertThrows(RuntimeException.class, () -> handler.execute(buildEvent("bucket", "bad.json")));
    }

    private void stubS3Download(String content) {
        GetObjectResponse response = GetObjectResponse.builder().build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response, content.getBytes());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
    }

    private S3Event buildEvent(String bucket, String key) {
        S3EventNotification.S3BucketEntity bucketEntity =
                new S3EventNotification.S3BucketEntity(bucket, null, null);
        S3EventNotification.S3ObjectEntity objectEntity =
                new S3EventNotification.S3ObjectEntity(key, 0L, null, null, null);
        S3EventNotification.S3Entity s3Entity =
                new S3EventNotification.S3Entity(null, bucketEntity, objectEntity, null);
        S3EventNotification.S3EventNotificationRecord record =
                new S3EventNotification.S3EventNotificationRecord(
                        null, null, null, null, null, null, null, s3Entity, null);
        return new S3Event(List.of(record));
    }

    private String singleRecipeJson() {
        return "[{\"name\":\"Pasta\",\"cuisine\":\"Italian\",\"prepTimeMinutes\":20,"
                + "\"ingredients\":[\"pasta\"],\"steps\":[\"boil\"]}]";
    }

    private String twoRecipesJson() {
        return "[{\"name\":\"Pasta\",\"cuisine\":\"Italian\",\"prepTimeMinutes\":20,"
                + "\"ingredients\":[\"pasta\"],\"steps\":[\"boil\"]},"
                + "{\"name\":\"Pizza\",\"cuisine\":\"Italian\",\"prepTimeMinutes\":30,"
                + "\"ingredients\":[\"dough\"],\"steps\":[\"bake\"]}]";
    }
}
