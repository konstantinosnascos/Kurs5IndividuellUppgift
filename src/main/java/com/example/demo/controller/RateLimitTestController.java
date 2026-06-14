package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateLimitTestController {

    @PostMapping("/test429")
    public ResponseEntity<String> test429() {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body("Rate limit");
    }
}