package com.group02.tars.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.tars.service.ServiceException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ZaiAiProvider implements AiProvider {
    private static final String DEFAULT_BASE_URL = "https://api.z.ai/api/paas/v4";
    private static final String DEFAULT_MODEL = "glm-4.6v";
    private static final int DEFAULT_TIMEOUT_SECONDS = 45;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    ZaiAiProvider() {
        this(
            firstNonBlank(System.getenv("TARS_AI_API_KEY"), System.getenv("AI_API_KEY")),
            firstNonBlank(System.getenv("TARS_AI_MODEL"), System.getenv("AI_MODEL"), DEFAULT_MODEL),
            firstNonBlank(System.getenv("TARS_AI_BASE_URL"), System.getenv("AI_BASE_URL"), DEFAULT_BASE_URL),
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
        );
    }

    ZaiAiProvider(String apiKey, String model, String baseUrl, HttpClient httpClient) {
        this.apiKey = normalize(apiKey);
        this.model = normalize(model).isBlank() ? DEFAULT_MODEL : normalize(model);
        this.baseUrl = normalize(baseUrl).isBlank() ? DEFAULT_BASE_URL : normalize(baseUrl);
        this.httpClient = httpClient;
    }

    @Override
    public boolean isReady() {
        return !apiKey.isBlank() && !model.isBlank() && !baseUrl.isBlank();
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("providerReady", isReady());
        status.put("mode", isReady() ? "provider-ready" : "tool-only");
        status.put("provider", "z.ai");
        status.put("model", model);
        status.put("baseUrlConfigured", !baseUrl.isBlank());
        status.put("multimodalCvReady", isReady() && model.toLowerCase().contains("v"));
        status.put("message", isReady()
            ? "Z.AI provider is configured and real model calls are enabled."
            : "No AI API key is configured. The system is returning deterministic tool output only.");
        return status;
    }

    @Override
    public AiProviderResult complete(String systemPrompt, String userPrompt, int maxTokens)
        throws IOException, ServiceException {
        if (!isReady()) {
            throw new ServiceException(503, "AI_PROVIDER_NOT_CONFIGURED", "AI provider is not configured.");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.2);
        requestBody.put("max_tokens", Math.max(64, maxTokens));
        requestBody.put("thinking", Map.of("type", "disabled"));

        HttpRequest request = HttpRequest.newBuilder(chatCompletionsUri())
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(502, "AI_PROVIDER_FAILED", "Z.AI request was interrupted.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ServiceException(502, "AI_PROVIDER_FAILED", "Z.AI request failed with HTTP " + response.statusCode() + ".");
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode firstChoice = root.path("choices").isArray() && !root.path("choices").isEmpty()
            ? root.path("choices").get(0)
            : mapper.createObjectNode();
        JsonNode message = firstChoice.path("message");
        String content = normalize(message.path("content").asText(""));
        String reasoning = normalize(message.path("reasoning_content").asText(""));
        if (content.isBlank() && !reasoning.isBlank()) {
            content = reasoning;
        }
        if (content.isBlank()) {
            throw new ServiceException(502, "AI_PROVIDER_EMPTY_RESPONSE", "Z.AI returned an empty response.");
        }

        Map<String, Object> usage = mapper.convertValue(root.path("usage"), new TypeReference<>() {});
        return new AiProviderResult(
            content,
            reasoning,
            root.path("model").asText(model),
            firstChoice.path("finish_reason").asText(""),
            usage
        );
    }

    private URI chatCompletionsUri() {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalized.endsWith("/chat/completions")) {
            return URI.create(normalized);
        }
        return URI.create(normalized + "/chat/completions");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
