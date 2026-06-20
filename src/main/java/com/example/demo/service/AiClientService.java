package com.example.demo.service;

import com.example.demo.dto.AiResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

//Denna används ej längre och kan tas bort, men lämnar kvar den så du kan se hur det va innan jag delade upp service och client.
//@Service
public class AiClientService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String SYSTEM_PROMPT =
            """
            You are a strict JSON API engine.

You must always return valid JSON.

RULES:
- Output must be valid JSON only
- No markdown
- No code blocks
- No explanations
- No additional keys

SCHEMA:
{
  "sentiment": "positive|negative|neutral",
  "score": integer (0-100)
}

If unsure:
- choose "neutral"
- set score to 0

Never deviate from schema.
""";



    public AiClientService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void validateApiKey(){
        if(apiKey == null || apiKey.isBlank()){
            throw new IllegalStateException("OpenAI API Key is missing");
        }
    }


    public AiResponseDto analyze(String text) {

        int retries = 3;
        long delay = 1000;

        for (int i = 0; i < retries; i++) {

            try {

                Map<String, Object> body = Map.of(
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

                String response = restClient.post()
                        .uri("https://api.openai.com/v1/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .body(body)
                        .retrieve()
                        .body(String.class);

                String aiContent = extractContent(response);

                return parseAndValidate(aiContent);

            } catch (Exception e) {

                if (e.getMessage() != null && e.getMessage().contains("429")) {

                    System.out.println("Rate limit hit. Retrying... " + (i + 1));

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {}

                    delay *= 2;
                    continue;
                }

                return fallback();
            }
        }

        return fallback();
    }

    private String extractContent(String response) {

        try {
            JsonNode root = objectMapper.readTree(response);

            return root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract AI content", e);
        }
    }

    private AiResponseDto parseAndValidate(String aiContent) {

        try {

            // 1. JSON → DTO
            AiResponseDto dto =
                    objectMapper.readValue(aiContent, AiResponseDto.class);

            // 2. Bean Validation
            Validator validator =
                    Validation.buildDefaultValidatorFactory()
                            .getValidator();

            var violations = validator.validate(dto);

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
                0,
                true
        );
    }
}
