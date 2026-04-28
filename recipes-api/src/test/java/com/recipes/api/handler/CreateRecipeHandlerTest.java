package com.recipes.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.exception.BadRequestException;
import com.recipes.api.generated.model.CreateRecipeRequest;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.service.RecipesService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRecipeHandlerTest {

    @Mock
    private RecipesService service;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private Validator validator;

    @InjectMocks
    private CreateRecipeHandler handler;

    @Test
    void returnsCreatedWithDetail() {
        when(validator.validate(any(CreateRecipeRequest.class))).thenReturn(Set.of());
        when(service.createRecipe(any(), any(), any(Integer.class), any(), any())).thenReturn(buildDetail());

        APIGatewayProxyResponseEvent response = handler.execute(requestWithBody(validBody()));

        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.getBody()).contains("\"id\":\"new-id\"");
    }

    @Test
    void returns400OnMalformedJson() {
        APIGatewayProxyResponseEvent response = handler.execute(requestWithBody("not-json{{{"));

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    @SuppressWarnings("unchecked")
    void returns400OnValidationViolation() {
        ConstraintViolation<CreateRecipeRequest> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");
        when(validator.validate(any(CreateRecipeRequest.class))).thenReturn(Set.of(violation));

        APIGatewayProxyResponseEvent response = handler.execute(requestWithBody(validBody()));

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("must not be blank");
    }

    @Test
    void returns400OnBadRequestException() {
        when(validator.validate(any(CreateRecipeRequest.class))).thenReturn(Set.of());
        when(service.createRecipe(any(), any(), any(Integer.class), any(), any()))
                .thenThrow(new BadRequestException("invalid input"));

        APIGatewayProxyResponseEvent response = handler.execute(requestWithBody(validBody()));

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    void returns500OnUnexpectedException() {
        when(validator.validate(any(CreateRecipeRequest.class))).thenReturn(Set.of());
        when(service.createRecipe(any(), any(), any(Integer.class), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        APIGatewayProxyResponseEvent response = handler.execute(requestWithBody(validBody()));

        assertThat(response.getStatusCode()).isEqualTo(500);
    }

    private APIGatewayProxyRequestEvent requestWithBody(String body) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(body);
        return request;
    }

    private String validBody() {
        return "{\"name\":\"Pasta\",\"cuisine\":\"Italian\",\"prepTimeMinutes\":20," +
                "\"ingredients\":[\"pasta\"],\"steps\":[\"boil\"]}";
    }

    private RecipeDetail buildDetail() {
        return RecipeDetail.builder()
                .id("new-id")
                .name("Pasta")
                .cuisine("Italian")
                .prepTimeMinutes(20)
                .ingredients(List.of("pasta"))
                .steps(List.of("boil"))
                .build();
    }
}
