package com.example.account.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    private String eventId;
    private String type;
    private double amount;
    private String currency;
    private Instant createdAt;

    public Transaction() {}
    public Transaction(String eventId, String type, double amount, String currency, Instant createdAt) {
        this.eventId = eventId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public String getEventId() { return eventId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
