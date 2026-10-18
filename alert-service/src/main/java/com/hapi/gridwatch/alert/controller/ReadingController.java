package com.hapi.gridwatch.alert.controller;


import com.hapi.gridwatch.alert.dto.WorkOrderDto;
import com.hapi.gridwatch.alert.service.ReadingService;
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

    @GetMapping(path = "work-orders")
    public ResponseEntity<List<WorkOrderDto>> workOrderList() {
        return ResponseEntity.ok(readingService.getAllWorkOrder());
    }
}
