package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AiResponseDto(
        @NotNull
        @Pattern(regexp = "positive|negative|neutral")
        String sentiment,

        @Min(0)
        @Max(100)
        int score,

        boolean error
)
{}
