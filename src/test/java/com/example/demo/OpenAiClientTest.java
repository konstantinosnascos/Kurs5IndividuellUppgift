package com.example.demo;

import com.example.demo.client.OpenAiClient;
import com.example.demo.dto.AiResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiClientTest {

    private MockWebServer mockWebServer;
    private OpenAiClient openAiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        Validator validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();

        openAiClient = new OpenAiClient(
                RestClient.builder().build(),
                new ObjectMapper(),
                validator
        );

        ReflectionTestUtils.setField(openAiClient, "apiKey", "test-key");
        ReflectionTestUtils.setField(openAiClient, "model", "gpt-4.1-mini");
        ReflectionTestUtils.setField(
                openAiClient,
                "apiUrl",
                mockWebServer.url("/v1/chat/completions").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnAiResponseWhenJsonIsValid() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"sentiment\\":\\"negative\\",\\"score\\":95}"
                      }
                    }
                  ]
                }
                """));

        AiResponseDto result = openAiClient.analyze(
                "This course is terrible and a complete waste of time."
        );

        assertEquals("negative", result.sentiment());
        assertEquals(95, result.score());
        assertFalse(result.error());
    }

    @Test
    void shouldReturnFallbackWhenAiReturnsBrokenJson() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "Sure, here is your summary..."
                      }
                    }
                  ]
                }
                """));

        AiResponseDto result = openAiClient.analyze("Test text");

        assertEquals("neutral", result.sentiment());
        assertEquals(0, result.score());
        assertTrue(result.error());
    }

    @Test
    void shouldReturnFallbackWhenAiReturnsInvalidSentiment() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"sentiment\\":\\"angry\\",\\"score\\":90}"
                      }
                    }
                  ]
                }
                """));

        AiResponseDto result = openAiClient.analyze("Test text");

        assertEquals("neutral", result.sentiment());
        assertEquals(0, result.score());
        assertTrue(result.error());
    }

    @Test
    void shouldRetryOn429AndThenSucceed() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"sentiment\\":\\"positive\\",\\"score\\":88}"
                      }
                    }
                  ]
                }
                """));

        AiResponseDto result = openAiClient.analyze("This is great!");

        assertEquals("positive", result.sentiment());
        assertEquals(88, result.score());
        assertFalse(result.error());
        assertEquals(2, mockWebServer.getRequestCount());
    }
}