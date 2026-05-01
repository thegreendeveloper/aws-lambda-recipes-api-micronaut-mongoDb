package com.recipes.api.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.recipes.api.model.RecipeEntity;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

@Factory
public class MongoClientFactory {

    @Singleton
    public MongoClient mongoClient() {
        String uri = System.getenv("MONGODB_URI");
        return MongoClients.create(uri != null && !uri.isBlank() ? uri : "mongodb://localhost:27017");
    }

    @Singleton
    public MongoDatabase mongoDatabase(MongoClient client) {
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        String db = System.getenv().getOrDefault("MONGODB_DATABASE", "recipes_service");
        return client.getDatabase(db).withCodecRegistry(pojoCodecRegistry);
    }

    @Singleton
    public MongoCollection<RecipeEntity> recipesCollection(MongoDatabase database) {
        return database.getCollection("recipes", RecipeEntity.class);
    }
}
