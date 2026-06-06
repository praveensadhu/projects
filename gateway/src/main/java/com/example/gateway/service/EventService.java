/**
 * @author "Venkata Praveen Kumar Gupta"
 */
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

        long startTime = System.currentTimeMillis();
        try {
            accountClient.applyTransaction(request.getAccountId(), request.getType().toUpperCase(), request.getAmount(), request.getCurrency(), request.getEventId())
                    .timeout(Duration.ofSeconds(2))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(200)).maxBackoff(Duration.ofSeconds(1)))
                    .block();
            
            // metricService.recordLatency(System.currentTimeMillis() - startTime);
            
            record.setStatus("SUBMITTED");
            repository.save(record);
            return record;
        } catch (Exception ex) {
            log.warn("Account service unavailable for event {}: {}", request.getEventId(), ex.getMessage());
            metricService.incrementAccountServiceFailure();
            // Keep as PENDING, but the save is already done above. 
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

    public Double getBalance(String accountId) {
        try {
            return accountClient.getBalance(accountId).block();
        } catch (Exception ex) {
            log.error("Account service unreachable for balance query: {}", accountId);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account Service unavailable for balance queries", ex);
        }
    }
}
