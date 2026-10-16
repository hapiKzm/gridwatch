package com.hapi.gridwatch.ingest.controller;


import com.hapi.gridwatch.ingest.dto.UserReadingRequest;
import com.hapi.gridwatch.ingest.service.ReadingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/readings")
public class ReadingController {

    private final ReadingService readingService;

    public ReadingController(ReadingService readingService) {
        this.readingService = readingService;
    }

    @PostMapping
    public ResponseEntity<Map<String, UUID>> receive(@Valid @RequestBody UserReadingRequest request) {
        UUID id = readingService.forward(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("id", id));
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<String> alertServiceDown() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Alert service is not reachable");
    }

    @ExceptionHandler(HttpClientErrorException.BadRequest.class)
    ResponseEntity<String> rejectedByAlertService(HttpClientErrorException.BadRequest e) {
        return ResponseEntity.badRequest().body(e.getResponseBodyAsString());
    }
}
