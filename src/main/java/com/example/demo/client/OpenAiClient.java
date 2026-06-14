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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

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
    
            EXAMPLES:
    
            Input:
            I love this product.
    
            Output:
            {"sentiment":"positive","score":95}
    
            Input:
            This is terrible.
    
            Output:
            {"sentiment":"negative","score":90}
    
            Input:
            The package arrived today.
    
            Output:
            {"sentiment":"neutral","score":70}
    
            Always follow the schema exactly.
            """;

    public OpenAiClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            Validator validator
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
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

                    log.warn(
                            "Rate limit hit. Retry {}/{}. Waiting {} ms",
                            i + 1,
                            retries,
                            delay
                    );

                    try {

                        Thread.sleep(delay);

                    } catch (InterruptedException ie) {

                        Thread.currentThread().interrupt();

                        log.error(
                                "Retry interrupted",
                                ie
                        );

                        return fallback();
                    }

                    delay *= 2;
                    continue;
                }

                log.error(
                        "OpenAI request failed",
                        e
                );

                return fallback();
            }
        }

        return fallback();
    }

    private Map<String, Object> buildRequestBody(String text) {

        return Map.of(
                "model", model,
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
                .uri(apiUrl)
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

                log.warn(
                        "Validation failed: {}",
                        violations
                );

                return fallback();
            }

            return new AiResponseDto(
                    dto.sentiment(),
                    dto.score(),
                    false
            );

        } catch (Exception e) {

            log.error(
                    "Failed to parse AI response: {}",
                    aiContent,
                    e
            );

            return fallback();
        }
    }

    private AiResponseDto fallback() {

        return new AiResponseDto(
                "neutral",
                0,
                true
        );
    }
}