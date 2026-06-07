package com.example.demo.service;

import com.example.demo.client.OpenAiClient;
import com.example.demo.dto.AiResponseDto;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final OpenAiClient openAiClient;

    public AiService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public AiResponseDto analyze(String text) {
        return openAiClient.analyze(text);
    }
}