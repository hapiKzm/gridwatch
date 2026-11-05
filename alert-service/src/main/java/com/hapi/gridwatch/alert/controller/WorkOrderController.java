package com.hapi.gridwatch.alert.controller;


import com.hapi.gridwatch.alert.dto.WorkOrderDto;
import com.hapi.gridwatch.alert.exception.WorkOrderNotFoundException;
import com.hapi.gridwatch.alert.service.ReadingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "work-orders")
public class WorkOrderController {

    private final ReadingService readingService;

    public WorkOrderController(ReadingService readingService) {
        this.readingService = readingService;
    }

    @GetMapping
    public ResponseEntity<List<WorkOrderDto>> workOrderList() {
        return ResponseEntity.ok(readingService.getAllWorkOrder());
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Map<String, Long>> closeWorkOrder(@PathVariable Long id) {
        readingService.closeWorkOrder(id);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("id", id));
    }

    @ExceptionHandler(WorkOrderNotFoundException.class)
    ResponseEntity<String> workOrderNotFound(WorkOrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

}
