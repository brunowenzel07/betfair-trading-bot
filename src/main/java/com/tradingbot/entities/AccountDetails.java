package com.tradingbot.entities;

public class AccountDetails {

    private double balance;
    private String currency;
    private String fullName;

    public AccountDetails(double balance, String currency, String fullName) {
        this.balance = balance;
        this.currency = currency;
        this.fullName = fullName;
    }

    public double getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getFullName() {
        return fullName;
    }
}
