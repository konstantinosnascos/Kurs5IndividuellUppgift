package com.example.demo.controller;

import com.example.demo.dto.AiResponseDto;
import com.example.demo.dto.UserRequestDto;
import com.example.demo.service.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/analyze")
    public AiResponseDto analyze(
            @Valid @RequestBody UserRequestDto request
    ) {
        return aiService.analyze(
                request.text()
        );
    }
}