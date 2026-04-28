package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.exception.BadRequestException;
import com.recipes.api.generated.model.CreateRecipeRequest;
import com.recipes.api.handler.mapper.RecipeDetailMapper;
import com.recipes.api.handler.support.LambdaResponseBuilder;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.service.RecipesService;
import io.micronaut.function.aws.MicronautRequestHandler;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;

public class CreateRecipeHandler
        extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    private RecipesService service;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private Validator validator;

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        try {
            CreateRecipeRequest request = parseRequest(input.getBody());
            String validationError = firstViolationMessage(validator.validate(request));
            if (validationError != null) {
                return LambdaResponseBuilder.error(objectMapper, 400, validationError);
            }
            RecipeDetail detail = service.createRecipe(
                    request.getName(), request.getCuisine(), request.getPrepTimeMinutes(),
                    request.getIngredients(), request.getSteps());
            String body = objectMapper.writeValueAsString(RecipeDetailMapper.toResponse(detail));
            return LambdaResponseBuilder.build(201, body);
        } catch (BadRequestException e) {
            return LambdaResponseBuilder.error(objectMapper, 400, e.getMessage());
        } catch (JsonProcessingException e) {
            return LambdaResponseBuilder.error(objectMapper, 400, "Invalid request body");
        } catch (Exception e) {
            return LambdaResponseBuilder.error(objectMapper, 500, "Internal server error");
        }
    }

    private CreateRecipeRequest parseRequest(String body) throws JsonProcessingException {
        return objectMapper.readValue(body, CreateRecipeRequest.class);
    }

    private String firstViolationMessage(Set<ConstraintViolation<CreateRecipeRequest>> violations) {
        if (violations.isEmpty()) {
            return null;
        }
        ConstraintViolation<CreateRecipeRequest> first = violations.iterator().next();
        return first.getPropertyPath() + ": " + first.getMessage();
    }
}
