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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ListRecipesHandler
        extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    private RecipesService service;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        try {
            List<RecipeSummary> summaries = parseIngredients(input)
                    .map(service::findRecipesByIngredients)
                    .orElseGet(service::listRecipes);
            return LambdaResponseBuilder.build(200, objectMapper.writeValueAsString(buildSummaryResponses(summaries)));
        } catch (Exception e) {
            return LambdaResponseBuilder.error(objectMapper, 500, "Internal server error");
        }
    }

    private Optional<List<String>> parseIngredients(APIGatewayProxyRequestEvent input) {
        Map<String, String> queryParams = input.getQueryStringParameters();
        if (queryParams == null) {
            return Optional.empty();
        }
        String param = queryParams.get("ingredients");
        if (param == null || param.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Arrays.stream(param.split(",")).map(String::trim).toList());
    }

    private List<RecipeSummaryResponse> buildSummaryResponses(List<RecipeSummary> summaries) {
        return summaries.stream()
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
