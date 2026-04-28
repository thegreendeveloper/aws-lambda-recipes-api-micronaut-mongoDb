package com.recipes.api.repository.impl;

import com.recipes.api.model.RecipeEntity;
import com.recipes.api.repository.RecipesRepository;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class DynamoDbRecipesRepository implements RecipesRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbRecipesRepository(DynamoDbClient dynamoDbClient,
                                     @Value("${RECIPES_TABLE_NAME:}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public List<RecipeEntity> findAll() {
        return scanAllItems().stream()
                .map(this::fromAttributeMap)
                .toList();
    }

    @Override
    public Optional<RecipeEntity> findById(String id) {
        Map<String, AttributeValue> item = fetchItemById(id);
        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromAttributeMap(item));
    }

    @Override
    public RecipeEntity save(RecipeEntity entity) {
        entity.setId(UUID.randomUUID().toString());
        dynamoDbClient.putItem(buildPutItemRequest(entity));
        return entity;
    }

    private List<Map<String, AttributeValue>> scanAllItems() {
        return dynamoDbClient.scan(ScanRequest.builder()
                        .tableName(tableName)
                        .build())
                .items();
    }

    private Map<String, AttributeValue> fetchItemById(String id) {
        return dynamoDbClient.getItem(GetItemRequest.builder()
                        .tableName(tableName)
                        .key(Map.of("id", AttributeValue.fromS(id)))
                        .build())
                .item();
    }

    private PutItemRequest buildPutItemRequest(RecipeEntity entity) {
        return PutItemRequest.builder()
                .tableName(tableName)
                .item(toAttributeMap(entity))
                .build();
    }

    private Map<String, AttributeValue> toAttributeMap(RecipeEntity entity) {
        List<AttributeValue> ingredients = toAttributeValueList(entity.getIngredients());
        List<AttributeValue> steps = toAttributeValueList(entity.getSteps());
        return Map.of(
                "id", AttributeValue.fromS(entity.getId()),
                "name", AttributeValue.fromS(entity.getName()),
                "cuisine", AttributeValue.fromS(entity.getCuisine()),
                "prepTimeMinutes", AttributeValue.fromN(String.valueOf(entity.getPrepTimeMinutes())),
                "ingredients", AttributeValue.fromL(ingredients),
                "steps", AttributeValue.fromL(steps)
        );
    }

    private RecipeEntity fromAttributeMap(Map<String, AttributeValue> item) {
        return RecipeEntity.builder()
                .id(item.get("id").s())
                .name(item.get("name").s())
                .cuisine(item.get("cuisine").s())
                .prepTimeMinutes(Integer.parseInt(item.get("prepTimeMinutes").n()))
                .ingredients(toStringList(item.get("ingredients")))
                .steps(toStringList(item.get("steps")))
                .build();
    }

    private List<AttributeValue> toAttributeValueList(List<String> values) {
        return values.stream()
                .map(AttributeValue::fromS)
                .toList();
    }

    private List<String> toStringList(AttributeValue attributeValue) {
        return attributeValue.l().stream()
                .map(AttributeValue::s)
                .toList();
    }
}
