package com.recipes.api.handler.support;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LambdaResponseBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ObjectMapper brokenMapper;

    @Test
    void buildSetsStatusBodyAndContentTypeHeader() {
        APIGatewayProxyResponseEvent response = LambdaResponseBuilder.build(200, "hello");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("hello");
        assertThat(response.getHeaders()).containsEntry("Content-Type", "application/json");
    }

    @Test
    void errorSerializesApiErrorWithStatusAndMessage() {
        APIGatewayProxyResponseEvent response = LambdaResponseBuilder.error(objectMapper, 404, "not found");

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getBody()).contains("\"status\":404");
        assertThat(response.getBody()).contains("\"message\":\"not found\"");
    }

    @Test
    void errorFallsBackToHardcodedBodyWhenSerializationFails() throws JsonProcessingException {
        when(brokenMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
        });

        APIGatewayProxyResponseEvent response = LambdaResponseBuilder.error(brokenMapper, 500, "msg");

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).isEqualTo("{\"status\":500,\"message\":\"Internal server error\"}");
    }
}
