package com.tradingbot.entities;

import java.util.ArrayList;
import java.util.List;

public class TradingContext {

    private List<TradingResult> tradingHistory = new ArrayList<>();
    private Double betUnit;
    private Double betAmount;
    private Double cycleBalance = 0d;

    public List<TradingResult> getTradingHistory() {
        return tradingHistory;
    }

    public void setTradingHistory(List<TradingResult> tradingHistory) {
        this.tradingHistory = tradingHistory;
    }

    public Double getBetUnit() {
        return betUnit;
    }

    public void setBetUnit(Double betUnit) {
        this.betUnit = betUnit;
    }

    public Double getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(Double betAmount) {
        this.betAmount = betAmount;
    }

    public Double getCycleBalance() {
        return cycleBalance;
    }

    public void setCycleBalance(Double cycleBalance) {
        this.cycleBalance = cycleBalance;
    }

    @Override
    public String toString() {
        return "TradingContext{" +
                "betUnit=" + betUnit +
                ", betAmount=" + betAmount +
                ", cycleBalance=" + cycleBalance +
                (tradingHistory.isEmpty() ? "" : ", lastProfit=" + tradingHistory.get(tradingHistory.size() -1).getProfit()) +
                '}';
    }
}
