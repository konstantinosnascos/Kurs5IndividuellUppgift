package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDto(
        @NotBlank
        @Size(max = 1000, message = "Text must be at most 1000 characters")
        String text
) {}
