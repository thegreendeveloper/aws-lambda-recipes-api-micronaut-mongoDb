package com.recipes.api.service;

import com.recipes.api.exception.RecipeNotFoundException;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.model.RecipeEntity;
import com.recipes.api.model.RecipeSummary;
import com.recipes.api.repository.RecipesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipesServiceUnitTest {

    @Mock
    private RecipesRepository repository;

    @InjectMocks
    private RecipesService service;

    @Test
    void listRecipesReturnsMappedSummaries() {
        when(repository.findAll()).thenReturn(List.of(buildTestEntity("abc")));

        List<RecipeSummary> result = service.listRecipes();

        assertThat(result).hasSize(1);
        RecipeSummary summary = result.get(0);
        assertThat(summary.getId()).isEqualTo("abc");
        assertThat(summary.getName()).isEqualTo("Pasta");
        assertThat(summary.getCuisine()).isEqualTo("Italian");
        assertThat(summary.getPrepTimeMinutes()).isEqualTo(20);
    }

    @Test
    void listRecipesReturnsEmptyListWhenNoRecipes() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listRecipes()).isEmpty();
    }

    @Test
    void getRecipeByIdReturnsMappedDetail() {
        when(repository.findById("abc")).thenReturn(Optional.of(buildTestEntity("abc")));

        RecipeDetail detail = service.getRecipeById("abc");

        assertThat(detail.getId()).isEqualTo("abc");
        assertThat(detail.getName()).isEqualTo("Pasta");
        assertThat(detail.getCuisine()).isEqualTo("Italian");
        assertThat(detail.getPrepTimeMinutes()).isEqualTo(20);
        assertThat(detail.getIngredients()).containsExactly("pasta", "sauce");
        assertThat(detail.getSteps()).containsExactly("boil", "mix");
    }

    @Test
    void getRecipeByIdThrowsWhenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RecipeNotFoundException.class, () -> service.getRecipeById("missing"));
    }

    @Test
    void findRecipesByIngredientsDelegatesToRepository() {
        when(repository.findByIngredients(List.of("pasta", "sauce")))
                .thenReturn(List.of(buildTestEntity("abc")));

        List<RecipeSummary> result = service.findRecipesByIngredients(List.of("pasta", "sauce"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("abc");
    }

    @Test
    void createRecipeCallsSaveAndReturnsMappedDetail() {
        RecipeEntity saved = buildTestEntity("new-id");
        when(repository.save(any())).thenReturn(saved);

        RecipeDetail detail = service.createRecipe("Pasta", "Italian", 20,
                List.of("pasta", "sauce"), List.of("boil", "mix"));

        verify(repository).save(any());
        assertThat(detail.getId()).isEqualTo("new-id");
        assertThat(detail.getName()).isEqualTo("Pasta");
        assertThat(detail.getIngredients()).containsExactly("pasta", "sauce");
        assertThat(detail.getSteps()).containsExactly("boil", "mix");
    }

    private RecipeEntity buildTestEntity(String id) {
        return RecipeEntity.builder()
                .id(id)
                .name("Pasta")
                .cuisine("Italian")
                .prepTimeMinutes(20)
                .ingredients(List.of("pasta", "sauce"))
                .steps(List.of("boil", "mix"))
                .build();
    }
}
