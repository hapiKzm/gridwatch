package com.hapi.gridwatch.alert.controller;


import com.hapi.gridwatch.alert.exception.UnknownSensorTypeException;
import com.hapi.gridwatch.alert.dto.ReadingRestRequest;
import com.hapi.gridwatch.alert.dto.WorkOrderDto;
import com.hapi.gridwatch.alert.service.ReadingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class ReadingController {

    private ReadingService readingService;

    public ReadingController(ReadingService readingService) {
        this.readingService = readingService;
    }

    @PostMapping(path = "reading/internal")
    public ResponseEntity<Object> readingController(@Valid @RequestBody ReadingRestRequest readingRestRequest) {
        try {
            readingService.saveReading(readingRestRequest);
        } catch (UnknownSensorTypeException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(readingRestRequest);
    }

    @GetMapping(path = "work-orders")
    public ResponseEntity<List<WorkOrderDto>> workOrderList() {
        return ResponseEntity.ok(readingService.getAllWorkOrder());
    }
}
