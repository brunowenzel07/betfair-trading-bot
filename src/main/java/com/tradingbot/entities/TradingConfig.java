package com.tradingbot.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class TradingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int minActiveRunners;
    private int countdownSeconds;
    private boolean inplay;
    private double minPrice;
    private double maxPrice;
    private double stakeSize; //percent from bank
    private double profitSize; //percent from layBet
    private String tradingEngine;
    private boolean showNegativeProfit;

    public TradingConfig(int minActiveRunners, int countdownSeconds, boolean inplay, double minPrice, double maxPrice, double stakeSize, double profitSize) {
        this.minActiveRunners = minActiveRunners;
        this.countdownSeconds = countdownSeconds;
        this.inplay = inplay;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.stakeSize = stakeSize;
        this.profitSize = profitSize;
    }

    public TradingConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getMinActiveRunners() {
        return minActiveRunners;
    }

    public void setMinActiveRunners(int minActiveRunners) {
        this.minActiveRunners = minActiveRunners;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }

    public boolean isInplay() {
        return inplay;
    }

    public void setInplay(boolean inplay) {
        this.inplay = inplay;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(double minPrice) {
        this.minPrice = minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public double getStakeSize() {
        return stakeSize;
    }

    public void setStakeSize(double stakeSize) {
        this.stakeSize = stakeSize;
    }

    public double getProfitSize() {
        return profitSize;
    }

    public void setProfitSize(double profitSize) {
        this.profitSize = profitSize;
    }

    public String getTradingEngine() {
        return tradingEngine;
    }

    public void setTradingEngine(String tradingEngine) {
        this.tradingEngine = tradingEngine;
    }

    public boolean isShowNegativeProfit() {
        return showNegativeProfit;
    }

    public void setShowNegativeProfit(boolean showNegativeProfit) {
        this.showNegativeProfit = showNegativeProfit;
    }
}