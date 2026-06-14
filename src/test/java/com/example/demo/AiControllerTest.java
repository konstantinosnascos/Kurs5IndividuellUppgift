package com.example.demo;

import com.example.demo.controller.AiController;
import com.example.demo.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiService aiService;

    @Test
    void shouldReturnBadRequestWhenTextIsBlank() throws Exception {
        mockMvc.perform(post("/api/ai/analyze")
                        .contentType("application/json")
                        .content("""
                        {
                          "text": ""
                        }
                        """))
                .andExpect(status().isBadRequest());
    }
}