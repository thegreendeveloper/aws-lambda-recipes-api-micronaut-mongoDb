package com.recipes.api.repository.impl;

import com.recipes.api.model.RecipeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipesRepositoryUnitTest {

    private static final String TABLE_NAME = "Recipes";
    private static final String RECIPE_ID = "abc";

    @Mock
    private DynamoDbClient dynamoDbClient;

    private DynamoDbRecipesRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DynamoDbRecipesRepository(dynamoDbClient, TABLE_NAME);
    }

    @Test
    void saveAssignsUuidToRecipe() {
        stubPutItem();
        RecipeEntity entity = buildTestEntity();

        RecipeEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotBlank();
    }

    @Test
    void saveWritesCorrectAttributesToDynamo() {
        stubPutItem();
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);

        repository.save(buildTestEntity());

        verify(dynamoDbClient).putItem(captor.capture());
        Map<String, AttributeValue> item = captor.getValue().item();
        assertThat(captor.getValue().tableName()).isEqualTo(TABLE_NAME);
        assertThat(item.get("name").s()).isEqualTo("Pasta");
        assertThat(item.get("cuisine").s()).isEqualTo("Italian");
        assertThat(item.get("prepTimeMinutes").n()).isEqualTo("20");
    }

    @Test
    void findByIdReturnsRecipeWhenItemExists() {
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(buildGetItemResponse());

        Optional<RecipeEntity> result = repository.findById("abc");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Pasta");
        assertThat(result.get().getCuisine()).isEqualTo("Italian");
    }

    @Test
    void findByIdReturnsEmptyWhenItemDoesNotExist() {
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        Optional<RecipeEntity> result = repository.findById("not-found");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllReturnsMappedRecipes() {
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(buildScanResponse());

        List<RecipeEntity> results = repository.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Pasta");
    }

    private void stubPutItem() {
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());
    }

    private RecipeEntity buildTestEntity() {
        return RecipeEntity.builder()
                .name("Pasta")
                .cuisine("Italian")
                .prepTimeMinutes(20)
                .ingredients(List.of("pasta", "sauce"))
                .steps(List.of("boil", "mix"))
                .build();
    }

    private GetItemResponse buildGetItemResponse() {
        return GetItemResponse.builder()
                .item(buildFakeAttributeMap())
                .build();
    }

    private ScanResponse buildScanResponse() {
        return ScanResponse.builder()
                .items(List.of(buildFakeAttributeMap()))
                .build();
    }

    private Map<String, AttributeValue> buildFakeAttributeMap() {
        return Map.of(
                "id", AttributeValue.fromS(RecipesRepositoryUnitTest.RECIPE_ID),
                "name", AttributeValue.fromS("Pasta"),
                "cuisine", AttributeValue.fromS("Italian"),
                "prepTimeMinutes", AttributeValue.fromN("20"),
                "ingredients", AttributeValue.fromL(List.of(AttributeValue.fromS("pasta"))),
                "steps", AttributeValue.fromL(List.of(AttributeValue.fromS("boil")))
        );
    }
}
