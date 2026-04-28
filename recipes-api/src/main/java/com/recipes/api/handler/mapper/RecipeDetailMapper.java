package com.recipes.api.handler.mapper;

import com.recipes.api.generated.model.RecipeDetailResponse;
import com.recipes.api.model.RecipeDetail;

public final class RecipeDetailMapper {

    private RecipeDetailMapper() {
    }

    public static RecipeDetailResponse toResponse(RecipeDetail detail) {
        return RecipeDetailResponse.builder()
                .id(detail.getId())
                .name(detail.getName())
                .cuisine(detail.getCuisine())
                .prepTimeMinutes(detail.getPrepTimeMinutes())
                .ingredients(detail.getIngredients())
                .steps(detail.getSteps())
                .build();
    }
}
