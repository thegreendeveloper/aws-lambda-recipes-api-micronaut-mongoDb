package com.recipes.api.repository.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.recipes.api.model.RecipeEntity;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipesRepositoryUnitTest {

    private static final String RECIPE_ID = "abc";

    @Mock
    private MongoCollection<RecipeEntity> collection;

    @Mock
    private FindIterable<RecipeEntity> findIterable;

    @InjectMocks
    private MongoDbRecipesRepository repository;

    @Test
    void saveAssignsUuidToRecipe() {
        RecipeEntity entity = buildTestEntity();

        RecipeEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotBlank();
    }

    @Test
    void saveCallsInsertOneWithEntity() {
        RecipeEntity entity = buildTestEntity();

        repository.save(entity);

        verify(collection).insertOne(entity);
    }

    @Test
    void findByIdReturnsRecipeWhenEntityExists() {
        RecipeEntity expected = buildTestEntity();
        expected.setId(RECIPE_ID);
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(expected);

        Optional<RecipeEntity> result = repository.findById(RECIPE_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Pasta");
        assertThat(result.get().getCuisine()).isEqualTo("Italian");
    }

    @Test
    void findByIdReturnsEmptyWhenEntityDoesNotExist() {
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(null);

        Optional<RecipeEntity> result = repository.findById("not-found");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllReturnsMappedRecipes() {
        RecipeEntity expected = buildTestEntity();
        when(collection.find()).thenReturn(findIterable);
        when(findIterable.into(any())).thenAnswer(inv -> {
            List<RecipeEntity> list = inv.getArgument(0);
            list.add(expected);
            return list;
        });

        List<RecipeEntity> results = repository.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Pasta");
    }

    @Test
    void findByIngredientsReturnsMatchingRecipes() {
        RecipeEntity expected = buildTestEntity();
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.into(any())).thenAnswer(inv -> {
            List<RecipeEntity> list = inv.getArgument(0);
            list.add(expected);
            return list;
        });

        List<RecipeEntity> results = repository.findByIngredients(List.of("pasta", "sauce"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Pasta");
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
}
