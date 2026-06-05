package com.example.gateway.controller;

import com.example.gateway.model.EventRecord;
import com.example.gateway.model.EventRequest;
import com.example.gateway.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/events")
    public ResponseEntity<?> submit(@Valid @RequestBody EventRequest request) {
        if (eventService.isDuplicate(request.getEventId())) {
            return ResponseEntity.ok(eventService.getEvent(request.getEventId()).orElseThrow());
        }
        try {
            EventRecord record = eventService.submitEvent(request);
            return ResponseEntity.status(record.getStatus().equals("PENDING") ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.CREATED).body(record);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Optional<EventRecord> record = eventService.getEvent(id);
        return record.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/events")
    public List<EventRecord> list(@RequestParam(name = "account", required = false) String account) {
        return eventService.listEvents(account);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("service", "gateway", "status", "UP");
    }
}
