package com.example.account.service;

import com.example.account.model.Account;
import com.example.account.model.Transaction;
import com.example.account.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account applyTransaction(String accountId, String type, double amount, String currency, String eventId) {
        Account account = accountRepository.findById(accountId).orElseGet(() -> new Account(accountId));
        double delta = "CREDIT".equalsIgnoreCase(type) ? amount : -amount;
        account.setBalance(account.getBalance() + delta);
        account.getTransactions().add(new Transaction(eventId, type, amount, currency, Instant.now()));
        return accountRepository.save(account);
    }

    public Optional<Account> getAccount(String accountId) {
        return accountRepository.findById(accountId);
    }

    public double getBalance(String accountId) {
        return accountRepository.findById(accountId).map(Account::getBalance).orElse(0.0);
    }
}
