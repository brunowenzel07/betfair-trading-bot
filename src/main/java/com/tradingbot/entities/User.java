package com.tradingbot.entities;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userName;
    private String fullName;
    private double balance;
    private String currency;
    private Date created;
    private Date lastLogin;
    private String ipAddress;
    private Date expiryDate;
    private boolean discountable = true;
    @OneToOne(cascade = CascadeType.ALL)
    private TradingConfig tradingConfig;
    @OneToOne(cascade = CascadeType.ALL)
    private Discount discount;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<BetReport> betReports = new ArrayList<>();

    public User(String userName, String fullName, double balance, String currency, String ipAddress, TradingConfig tradingConfig) {
        this.userName = userName;
        this.fullName = fullName;
        this.balance = balance;
        this.currency = currency;
        this.ipAddress = ipAddress;
        this.tradingConfig = tradingConfig;
        Date now = new Date();
        this.created = now;
        this.lastLogin = now;
        this.expiryDate = now;
    }

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isDiscountable() {
        return discountable;
    }

    public void setDiscountable(boolean discountable) {
        this.discountable = discountable;
    }

    public TradingConfig getTradingConfig() {
        return tradingConfig;
    }

    public void setTradingConfig(TradingConfig tradingConfig) {
        this.tradingConfig = tradingConfig;
    }

    public Discount getDiscount() {
        return discount;
    }

    public void setDiscount(Discount discount) {
        this.discount = discount;
    }

    public List<BetReport> getBetReports() {
        return betReports;
    }

    public void setBetReports(List<BetReport> betReports) {
        this.betReports = betReports;
    }
}
