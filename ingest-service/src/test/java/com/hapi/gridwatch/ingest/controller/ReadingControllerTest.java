package com.hapi.gridwatch.ingest.controller;


import com.hapi.gridwatch.ingest.config.WebSecurityConfig;
import com.hapi.gridwatch.ingest.exception.InvalidReadingException;
import com.hapi.gridwatch.ingest.exception.KafkaUnavailableException;
import com.hapi.gridwatch.ingest.service.ReadingService;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReadingController.class)
@Import(WebSecurityConfig.class)
public class ReadingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReadingService readingService;

    private static final String VALID_JSON = """
        {"substationId":"DEV-01","sensorType":"TEMPERATURE","value":95.0,"measuredAt":"2026-10-30T12:00:00Z"}
        """;
    private static final String INVALID_JSON = """
        {"substationId":"DEV-01"}
        """;

    @Test
    void noLoginReturns401() throws Exception {
        mockMvc
                .perform(
                        post("/api/readings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                ).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void validReadingReturns202() throws Exception{
        when(readingService.publish(any())).thenReturn(UUID.randomUUID());

        mockMvc.perform(post("/api/readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists());

    }

    @Test
    @WithMockUser
    void missingFieldReturns400() throws Exception{
        mockMvc.perform(post("/api/readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_JSON))
                .andExpect(status().isBadRequest());

        verify(readingService, never()).publish(any());
    }

    @Test
    @WithMockUser
    void unknownSensorReturns400() throws Exception {
        when(readingService.publish(any())).thenThrow(new InvalidReadingException("Unknown sensor type"));

        mockMvc.perform(post("/api/readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void kafkaDownReturns503() throws Exception {
        when(readingService.publish(any())).thenThrow(new KafkaUnavailableException("down", new RuntimeException()));

        mockMvc.perform(post("/api/readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isServiceUnavailable());
    }


}
