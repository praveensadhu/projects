package com.example.gateway.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "event_records")
public class EventRecord {
    @Id
    private String eventId;
    private String accountId;
    private String type;
    private double amount;
    private String currency;
    private Instant eventTimestamp;
    private String metadata;
    private Instant createdAt;
    private String status;

    public EventRecord() {}

    public EventRecord(String eventId, String accountId, String type, double amount, String currency,
                       Instant eventTimestamp, String metadata, Instant createdAt, String status) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getEventId() { return eventId; }
    public String getAccountId() { return accountId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
