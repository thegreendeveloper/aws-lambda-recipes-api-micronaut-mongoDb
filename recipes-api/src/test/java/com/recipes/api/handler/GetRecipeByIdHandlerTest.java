package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.exception.RecipeNotFoundException;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.service.RecipesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRecipeByIdHandlerTest {

    @Mock
    private RecipesService service;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private GetRecipeByIdHandler handler;

    @Test
    void returnsOkWithDetail() {
        when(service.getRecipeById("abc")).thenReturn(buildDetail());

        APIGatewayProxyResponseEvent response = handler.execute(requestWithId("abc"));

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"id\":\"abc\"").contains("pasta").contains("boil");
    }

    @Test
    void returns404WhenNotFound() {
        when(service.getRecipeById("missing")).thenThrow(new RecipeNotFoundException("missing"));

        APIGatewayProxyResponseEvent response = handler.execute(requestWithId("missing"));

        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    void returns500OnUnexpectedException() {
        when(service.getRecipeById("abc")).thenThrow(new RuntimeException("boom"));

        APIGatewayProxyResponseEvent response = handler.execute(requestWithId("abc"));

        assertThat(response.getStatusCode()).isEqualTo(500);
    }

    private APIGatewayProxyRequestEvent requestWithId(String id) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPathParameters(Map.of("id", id));
        return request;
    }

    private RecipeDetail buildDetail() {
        return RecipeDetail.builder()
                .id("abc")
                .name("Pasta")
                .cuisine("Italian")
                .prepTimeMinutes(20)
                .ingredients(List.of("pasta", "sauce"))
                .steps(List.of("boil", "mix"))
                .build();
    }
}
