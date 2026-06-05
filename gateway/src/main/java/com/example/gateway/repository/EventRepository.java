package com.example.gateway.repository;

import com.example.gateway.model.EventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<EventRecord, String> {
    Optional<EventRecord> findByEventId(String eventId);
    List<EventRecord> findByAccountIdOrderByEventTimestampAsc(String accountId);
}
