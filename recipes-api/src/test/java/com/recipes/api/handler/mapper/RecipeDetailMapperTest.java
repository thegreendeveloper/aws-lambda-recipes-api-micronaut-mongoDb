package com.recipes.api.handler.mapper;

import com.recipes.api.generated.model.RecipeDetailResponse;
import com.recipes.api.model.RecipeDetail;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeDetailMapperTest {

    @Test
    void toResponseMapsAllFields() {
        RecipeDetail detail = RecipeDetail.builder()
                .id("abc")
                .name("Pasta")
                .cuisine("Italian")
                .prepTimeMinutes(20)
                .ingredients(List.of("pasta", "sauce"))
                .steps(List.of("boil", "mix"))
                .build();

        RecipeDetailResponse response = RecipeDetailMapper.toResponse(detail);

        assertThat(response.getId()).isEqualTo("abc");
        assertThat(response.getName()).isEqualTo("Pasta");
        assertThat(response.getCuisine()).isEqualTo("Italian");
        assertThat(response.getPrepTimeMinutes()).isEqualTo(20);
        assertThat(response.getIngredients()).containsExactly("pasta", "sauce");
        assertThat(response.getSteps()).containsExactly("boil", "mix");
    }
}
