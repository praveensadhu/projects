package com.example.account.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private String accountId;
    private double balance;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "account_id")
    private List<Transaction> transactions = new ArrayList<>();

    public Account() {}
    public Account(String accountId) { this.accountId = accountId; }

    public String getAccountId() { return accountId; }
    public double getBalance() { return balance; }
    public List<Transaction> getTransactions() { return transactions; }
    public void setBalance(double balance) { this.balance = balance; }
}
