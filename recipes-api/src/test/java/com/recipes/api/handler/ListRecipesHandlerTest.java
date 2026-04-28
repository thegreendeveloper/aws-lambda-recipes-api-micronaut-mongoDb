package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.model.RecipeSummary;
import com.recipes.api.service.RecipesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRecipesHandlerTest {

    @Mock
    private RecipesService service;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private ListRecipesHandler handler;

    @Test
    void returnsOkWithMappedSummaries() {
        when(service.listRecipes()).thenReturn(List.of(buildSummary()));

        APIGatewayProxyResponseEvent response = handler.execute(new APIGatewayProxyRequestEvent());

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("Pasta").contains("Italian").contains("abc");
    }

    @Test
    void returnsEmptyArrayWhenNoRecipes() {
        when(service.listRecipes()).thenReturn(List.of());

        APIGatewayProxyResponseEvent response = handler.execute(new APIGatewayProxyRequestEvent());

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    @Test
    void returns500OnUnexpectedException() {
        when(service.listRecipes()).thenThrow(new RuntimeException("boom"));

        APIGatewayProxyResponseEvent response = handler.execute(new APIGatewayProxyRequestEvent());

        assertThat(response.getStatusCode()).isEqualTo(500);
    }

    private RecipeSummary buildSummary() {
        return RecipeSummary.builder()
                .id("abc")
                .name("Pasta")
                .cuisine("Italian")
                .prepTimeMinutes(20)
                .build();
    }
}
