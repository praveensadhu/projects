package com.example.account.controller;

import com.example.account.model.Account;
import com.example.account.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AccountController {
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<?> apply(@PathVariable String accountId,
                                   @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                   @RequestBody Map<String, Object> body) {
        if (traceId != null) MDC.put("traceId", traceId);
        log.info("Applying transaction for account {} traceId={}", accountId, traceId);
        String type = String.valueOf(body.get("type"));
        double amount = Double.parseDouble(String.valueOf(body.get("amount")));
        String currency = String.valueOf(body.get("currency"));
        String eventId = String.valueOf(body.get("eventId"));
        Account account = accountService.applyTransaction(accountId, type, amount, currency, eventId);
        return ResponseEntity.ok(Map.of("status", "OK", "balance", account.getBalance()));
    }

    @GetMapping("/accounts/{accountId}/balance")
    public Map<String, Object> balance(@PathVariable String accountId) {
        return Map.of("accountId", accountId, "balance", accountService.getBalance(accountId));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<?> details(@PathVariable String accountId) {
        return accountService.getAccount(accountId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("service", "account-service", "status", "UP");
    }
}
