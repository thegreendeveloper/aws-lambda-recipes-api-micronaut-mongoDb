package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.generated.model.RecipeSummaryResponse;
import com.recipes.api.handler.support.LambdaResponseBuilder;
import com.recipes.api.model.RecipeSummary;
import com.recipes.api.service.RecipesService;
import io.micronaut.function.aws.MicronautRequestHandler;
import jakarta.inject.Inject;

import java.util.List;

public class ListRecipesHandler
        extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    private RecipesService service;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        try {
            return LambdaResponseBuilder.build(200, objectMapper.writeValueAsString(buildSummaryResponses()));
        } catch (Exception e) {
            return LambdaResponseBuilder.error(objectMapper, 500, "Internal server error");
        }
    }

    private List<RecipeSummaryResponse> buildSummaryResponses() {
        return service.listRecipes().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    private RecipeSummaryResponse toSummaryResponse(RecipeSummary summary) {
        return RecipeSummaryResponse.builder()
                .id(summary.getId())
                .name(summary.getName())
                .cuisine(summary.getCuisine())
                .prepTimeMinutes(summary.getPrepTimeMinutes())
                .build();
    }
}
