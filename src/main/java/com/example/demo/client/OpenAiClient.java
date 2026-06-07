package com.example.demo.client;

import com.example.demo.dto.AiResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String SYSTEM_PROMPT =
            """
            You are a sentiment analysis engine.

            Return ONLY valid JSON.

            RULES:
            - No markdown
            - No code blocks
            - No explanations
            - No additional text

            SCHEMA:
            {
              "sentiment":"positive|negative|neutral",
              "score":0-100
            }

            Always follow the schema exactly.
            """;

    public OpenAiClient(
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.validator =
                Validation.buildDefaultValidatorFactory()
                        .getValidator();
    }

    @PostConstruct
    public void validateApiKey() {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "CRITICAL: API key is missing."
            );
        }
    }

    public AiResponseDto analyze(String text) {

        int retries = 3;
        long delay = 1000;

        for (int i = 0; i < retries; i++) {

            try {

                Map<String, Object> body = buildRequestBody(text);

                String response = callOpenAi(body);

                String content = extractContent(response);

                return parseAndValidate(content);

            } catch (Exception e) {

                if (e.getMessage() != null &&
                        e.getMessage().contains("429")) {

                    System.out.println(
                            "Rate limit hit. Retrying..."
                    );

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {
                    }

                    delay *= 2;
                    continue;
                }

                return fallback();
            }
        }

        return fallback();
    }

    private Map<String, Object> buildRequestBody(String text) {

        return Map.of(
                "model", "gpt-4.1-mini",
                "temperature", 0.1,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", SYSTEM_PROMPT
                        ),
                        Map.of(
                                "role", "user",
                                "content", text
                        )
                )
        );
    }

    private String callOpenAi(Map<String, Object> body) {

        return restClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization",
                        "Bearer " + apiKey)
                .header("Content-Type",
                        "application/json")
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private String extractContent(String response) {

        try {

            JsonNode root =
                    objectMapper.readTree(response);

            return root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to extract AI content",
                    e
            );
        }
    }

    private AiResponseDto parseAndValidate(
            String aiContent
    ) {

        try {

            AiResponseDto dto =
                    objectMapper.readValue(
                            aiContent,
                            AiResponseDto.class
                    );

            var violations =
                    validator.validate(dto);

            if (!violations.isEmpty()) {
                return fallback();
            }

            return dto;

        } catch (Exception e) {

            return fallback();
        }
    }

    private AiResponseDto fallback() {

        return new AiResponseDto(
                "neutral",
                0
        );
    }
}