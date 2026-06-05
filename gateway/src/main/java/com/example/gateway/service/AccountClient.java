package com.example.gateway.service;

import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AccountClient {
    private final WebClient webClient;

    public AccountClient(WebClient accountWebClient) {
        this.webClient = accountWebClient;
    }

    public Mono<Void> applyTransaction(String accountId, String type, double amount, String currency, String eventId) {
        String traceId = MDC.get("traceId");
        return webClient.post()
                .uri("/accounts/{accountId}/transactions", accountId)
                .header("X-Trace-Id", traceId)
                .bodyValue(new TransactionRequest(type, amount, currency, eventId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class).defaultIfEmpty("").map(msg -> new RuntimeException("Account service error: " + response.statusCode() + " " + msg)))
                .toBodilessEntity()
                .then();
    }

    public record TransactionRequest(String type, double amount, String currency, String eventId) {}
}
