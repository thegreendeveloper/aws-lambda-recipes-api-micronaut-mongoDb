package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.exception.RecipeNotFoundException;
import com.recipes.api.handler.mapper.RecipeDetailMapper;
import com.recipes.api.handler.support.LambdaResponseBuilder;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.service.RecipesService;
import io.micronaut.function.aws.MicronautRequestHandler;
import jakarta.inject.Inject;

public class GetRecipeByIdHandler
        extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    private RecipesService service;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        try {
            String id = input.getPathParameters().get("id");
            RecipeDetail detail = service.getRecipeById(id);
            String body = objectMapper.writeValueAsString(RecipeDetailMapper.toResponse(detail));
            return LambdaResponseBuilder.build(200, body);
        } catch (RecipeNotFoundException e) {
            return LambdaResponseBuilder.error(objectMapper, 404, e.getMessage());
        } catch (Exception e) {
            return LambdaResponseBuilder.error(objectMapper, 500, "Internal server error");
        }
    }
}
