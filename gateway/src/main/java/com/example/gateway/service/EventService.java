package com.example.gateway.service;

import com.example.gateway.model.EventRecord;
import com.example.gateway.model.EventRequest;
import com.example.gateway.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final EventRepository repository;
    private final AccountClient accountClient;
    private final MetricService metricService;

    public EventService(EventRepository repository, AccountClient accountClient, MetricService metricService) {
        this.repository = repository;
        this.accountClient = accountClient;
        this.metricService = metricService;
    }

    public boolean isDuplicate(String eventId) {
        return repository.findByEventId(eventId).isPresent();
    }

    public EventRecord submitEvent(EventRequest request) {
        if (!"CREDIT".equalsIgnoreCase(request.getType()) && !"DEBIT".equalsIgnoreCase(request.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown event type");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }
        Optional<EventRecord> existing = repository.findByEventId(request.getEventId());
        if (existing.isPresent()) {
            return existing.get();
        }

        EventRecord record = new EventRecord(request.getEventId(), request.getAccountId(), request.getType().toUpperCase(),
                request.getAmount(), request.getCurrency(), request.getEventTimestamp(), request.getMetadata(), Instant.now(), "PENDING");
        repository.save(record);
        metricService.incrementEventSubmission();

        try {
            accountClient.applyTransaction(request.getAccountId(), request.getType().toUpperCase(), request.getAmount(), request.getCurrency(), request.getEventId())
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(200)).maxBackoff(Duration.ofSeconds(1)))
                    .doOnError(error -> {
                        log.warn("Account service unavailable for event {}", request.getEventId(), error);
                        metricService.incrementAccountServiceFailure();
                    })
                    .block();
            record.setStatus("SUBMITTED");
            repository.save(record);
            return record;
        } catch (Exception ex) {
            record.setStatus("PENDING");
            repository.save(record);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account Service unavailable", ex);
        }
    }

    public Optional<EventRecord> getEvent(String id) {
        return repository.findByEventId(id);
    }

    public List<EventRecord> listEvents(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return repository.findAll();
        }
        return repository.findByAccountIdOrderByEventTimestampAsc(accountId);
    }
}
