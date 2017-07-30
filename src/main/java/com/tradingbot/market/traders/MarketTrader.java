package com.tradingbot.market.traders;


import com.betfair.aping.exceptions.APINGException;
import com.tradingbot.entities.TradingResult;

public interface MarketTrader {
    TradingResult trade() throws Exception, APINGException;
}
