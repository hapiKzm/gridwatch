package com.hapi.gridwatch.ingest.controller;


import com.hapi.gridwatch.ingest.exception.KafkaUnavailableException;
import com.hapi.gridwatch.ingest.dto.UserReadingRequest;
import com.hapi.gridwatch.ingest.exception.InvalidReadingException;
import com.hapi.gridwatch.ingest.service.ReadingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        UUID id = readingService.publish(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("id", id));
    }

    @ExceptionHandler(KafkaUnavailableException.class)
    ResponseEntity<String> kafkaDown(KafkaUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
    }

    @ExceptionHandler(InvalidReadingException.class)
    ResponseEntity<String> badInput(InvalidReadingException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
