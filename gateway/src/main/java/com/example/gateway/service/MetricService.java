package com.example.gateway.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricService {
    private final MeterRegistry meterRegistry;

    public MetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementEventSubmission() {
        meterRegistry.counter("gateway.events.submitted").increment();
    }

    public void incrementAccountServiceFailure() {
        meterRegistry.counter("gateway.account.service.failures").increment();
    }
}
