package com.recipes.api.handler.support;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.api.generated.model.ApiError;

import java.time.OffsetDateTime;
import java.util.Map;

public final class LambdaResponseBuilder {

    private LambdaResponseBuilder() {
    }

    public static APIGatewayProxyResponseEvent build(int statusCode, String body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody(body);
        response.setHeaders(Map.of("Content-Type", "application/json"));
        return response;
    }

    public static APIGatewayProxyResponseEvent error(ObjectMapper objectMapper, int statusCode, String message) {
        ApiError error = new ApiError();
        error.setStatus(statusCode);
        error.setMessage(message);
        error.setTimestamp(OffsetDateTime.now());
        try {
            return build(statusCode, objectMapper.writeValueAsString(error));
        } catch (JsonProcessingException e) {
            return build(500, "{\"status\":500,\"message\":\"Internal server error\"}");
        }
    }
}
