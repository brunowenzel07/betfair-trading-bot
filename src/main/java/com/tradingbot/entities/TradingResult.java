package com.tradingbot.entities;

public class TradingResult {

    private Double profit;
    private String selection;
    private Double laySize;
    private Double layPrice;
    private Double backSize;
    private Double backPrice;

    public TradingResult(Double profit, String selection, Double laySize, Double layPrice, Double backSize, Double backPrice) {
        this.profit = profit;
        this.selection = selection;
        this.laySize = laySize;
        this.layPrice = layPrice;
        this.backSize = backSize;
        this.backPrice = backPrice;
    }

    public TradingResult(Double profit) {
        this.profit = profit;
    }

    public Double getProfit() {
        return profit;
    }

    public String getSelection() {
        return selection;
    }

    public Double getLaySize() {
        return laySize;
    }

    public Double getLayPrice() {
        return layPrice;
    }

    public Double getBackSize() {
        return backSize;
    }

    public Double getBackPrice() {
        return backPrice;
    }
}
