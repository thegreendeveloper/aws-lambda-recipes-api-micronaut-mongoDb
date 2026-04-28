package com.recipes.api.repository.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.recipes.api.model.RecipeEntity;
import com.recipes.api.repository.RecipesRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MongoDbRecipesRepository implements RecipesRepository {

    @Inject
    private MongoCollection<RecipeEntity> collection;

    @Override
    public List<RecipeEntity> findAll() {
        return collection.find().into(new ArrayList<>());
    }

    @Override
    public Optional<RecipeEntity> findById(String id) {
        return Optional.ofNullable(collection.find(Filters.eq("_id", id)).first());
    }

    @Override
    public RecipeEntity save(RecipeEntity entity) {
        entity.setId(UUID.randomUUID().toString());
        collection.insertOne(entity);
        return entity;
    }

    @Override
    public List<RecipeEntity> findByIngredients(List<String> ingredients) {
        return collection.find(Filters.all("ingredients", ingredients)).into(new ArrayList<>());
    }
}
