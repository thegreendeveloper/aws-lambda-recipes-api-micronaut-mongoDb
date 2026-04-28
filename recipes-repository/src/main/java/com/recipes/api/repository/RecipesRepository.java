package com.recipes.api.repository;

import com.recipes.api.model.RecipeEntity;

import java.util.List;
import java.util.Optional;

public interface RecipesRepository {
    List<RecipeEntity> findAll();

    Optional<RecipeEntity> findById(String id);

    RecipeEntity save(RecipeEntity entity);

    List<RecipeEntity> findByIngredients(List<String> ingredients);
}
