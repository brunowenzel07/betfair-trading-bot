package com.tradingbot.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
public class BetReport {

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String marketName;
    private Date startTime;
    private Double profit;
    private String selection;
    private Double laySize;
    private Double layPrice;
    private Double backSize;
    private Double backPrice;

    public BetReport(
            String marketName, Date startTime, Double profit, String selection, Double laySize, Double layPrice,
                    Double backSize, Double backPrice) {
        this.marketName = marketName;
        this.startTime = startTime;
        this.profit = profit;
        this.selection = selection;
        this.laySize = laySize;
        this.layPrice = layPrice;
        this.backSize = backSize;
        this.backPrice = backPrice;
    }

    public BetReport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Double getProfit() {
        return profit;
    }

    public void setProfit(Double profit) {
        this.profit = profit;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public Double getLaySize() {
        return laySize;
    }

    public void setLaySize(Double laySize) {
        this.laySize = laySize;
    }

    public Double getLayPrice() {
        return layPrice;
    }

    public void setLayPrice(Double layPrice) {
        this.layPrice = layPrice;
    }

    public Double getBackSize() {
        return backSize;
    }

    public void setBackSize(Double backSize) {
        this.backSize = backSize;
    }

    public Double getBackPrice() {
        return backPrice;
    }

    public void setBackPrice(Double backPrice) {
        this.backPrice = backPrice;
    }
}
