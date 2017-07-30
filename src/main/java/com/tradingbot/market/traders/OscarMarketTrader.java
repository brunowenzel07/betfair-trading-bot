package com.tradingbot.market.traders;

import com.betfair.aping.entities.*;
import com.betfair.aping.enums.InstructionReportStatus;
import com.betfair.aping.enums.PriceData;
import com.betfair.aping.enums.Side;
import com.betfair.aping.exceptions.APINGException;
import com.betfair.aping.util.SessionTokenDiscoverer;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tradingbot.CalcUtils;
import com.tradingbot.entities.TradingResult;
import com.tradingbot.entities.TradingConfig;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tradingbot.BetfairUtils.APP_KEY;

public class OscarMarketTrader extends AbstractMarketTrader {

    public static final String NAME = "oscar-lay";
    private static final Logger LOG = Logger.getLogger(OscarMarketTrader.class);
    private static final Map<String , Integer> distanceRunnerCountMap = new HashMap<String , Integer>(){{
        put("1m", 7);
        put("2m", 6);
        put("3m", 5);
    }};

    public OscarMarketTrader(MarketCatalogue market, TradingConfig tradingConfig, SessionTokenDiscoverer tokenDiscoverer) {
        super(market, tradingConfig, tokenDiscoverer);
    }

    @Override
    public TradingResult trade() throws Exception, APINGException {
        String eventName = getEventName(market);
        if(tradingConfig.isInplay()) {
            logMessage(String.format("Waiting for in play '%s'", eventName));
            waitForInplay(market.getMarketId());
            logMessage(String.format("In play started '%s'", eventName));
        }
        List<Runner> activeRunners = getActiveRunners(market.getMarketId());
        int activeRunnersCount = activeRunners.size();
        String distance = getDistance(market).substring(0, 2);
        Integer minRunnerCount = distanceRunnerCountMap.get(distance);
        if(!(activeRunnersCount > minRunnerCount)) {
            logMessage("Market is not suitable for trading.");
            LOG.debug(String.format("Market is not suitable by active runners count=%d; distance %s", activeRunnersCount, distance));
            return null;
        }
        if(hasRunnerWithLowestPrice(market.getMarketId(), tradingConfig.getMinPrice())) {
            logMessage("Market is not suitable for trading.");
            LOG.debug("Skip by has runner with lowest price");
            return null;
        }
        logMessage(String.format("Start trading event %s", getEventName(market)));
        LOG.debug("Getting selectionId ...");
        Long selectionId = getSelectionId(market.getMarketId());
        while (selectionId == null) {
            LOG.warn("SelectionId is null");
            selectionId = getSelectionId(market.getMarketId());
        }
        updateTradingContext();
        LOG.debug("Start: "+tradingContext);
        Double betAmount = tradingContext.getBetAmount();
        LOG.debug(String.format("Placing LAY bet at %s; amount %s; price %s ...", getRunnerName(selectionId), betAmount, tradingConfig.getMaxPrice()));
        PlaceInstructionReport report = placeBet(market.getMarketId(), selectionId, Side.LAY, betAmount, tradingConfig.getMaxPrice());
        if(report == null || !report.getStatus().equals(InstructionReportStatus.SUCCESS)) {
            LOG.error("Failed to place LAY bet.");
            return null;
        }
        logMessage(String.format("LAY bet placed successfully: size %s; avgPrice %s", report.getSizeMatched(), report.getAveragePriceMatched()));
        TimeUnit.MINUTES.sleep(1);
        Long winner = getWinner(market.getMarketId());
        LOG.debug(String.format("Winner selectionId=%s; name=%s", winner, getRunnerName(winner)));
        CurrentOrderSummary orderSummary = getOrderSummary(report.getBetId());
        if(orderSummary.getSizeMatched() == 0) {
            logMessage("Bet has not been matched.");
            return null;
        }
        double avgMatchedPrice = orderSummary.getAveragePriceMatched();
        double laySize = orderSummary.getSizeMatched();
        double layPrice = CalcUtils.round(avgMatchedPrice, 2);
        double profit = winner.equals(selectionId) ?
                -CalcUtils.calculateLiabilitySize(avgMatchedPrice, laySize) : CalcUtils.round(laySize * 0.935, 2);
        if(winner == 0) {
            LOG.warn("Cannot obtain winner selectionId");
            ClearedOrderSummary marketSummary = getClearedOrderSummary(market.getMarketId(), null);
            ClearedOrderSummary layBetSummary = getClearedOrderSummary(market.getMarketId(), report.getBetId());
            laySize = layBetSummary.getSizeSettled();
            layPrice = layBetSummary.getPriceMatched();
            profit = CalcUtils.round(marketSummary.getProfit() - marketSummary.getCommission(), 2);
        }
        String runnerName = getRunnerName(selectionId);
        tradingContext.setCycleBalance(CalcUtils.round(tradingContext.getCycleBalance() + profit, 2));
        tradingContext.getTradingHistory().add(new TradingResult(profit));
        LOG.debug("Finish: "+tradingContext);
        return new TradingResult(profit, runnerName, laySize, layPrice, null, null);
    }

    private boolean hasRunnerWithLowestPrice(String marketId, double minPrice) throws Exception, APINGException {
        ArrayList<String> marketIds = Lists.newArrayList(marketId);
        String sessionToken = tokenDiscoverer.getSessionToken();
        PriceProjection priceProjection = new PriceProjection();
        priceProjection.setPriceData(Sets.newHashSet(PriceData.EX_BEST_OFFERS));
        List<MarketBook> marketBooks = bfApi.listMarketBook(marketIds, priceProjection, null, null, "USD", APP_KEY, sessionToken);
        for (Runner runner : marketBooks.get(0).getRunners()) {
            if(!"ACTIVE".equals(runner.getStatus())) {
                continue;
            }
            ExchangePrices ex = runner.getEx();
            List<PriceSize> availableToLay = ex.getAvailableToLay();
            if(availableToLay.isEmpty()) {
                continue;
            }
            if(availableToLay.get(0).getPrice() <= minPrice) {
                return true;
            }
        }
        return false;
    }

    private Long getSelectionId(String marketId) throws Exception, APINGException {
        ArrayList<String> marketIds = Lists.newArrayList(marketId);
        String sessionToken = tokenDiscoverer.getSessionToken();
        PriceProjection priceProjection = new PriceProjection();
        priceProjection.setPriceData(Sets.newHashSet(PriceData.EX_BEST_OFFERS));
        List<MarketBook> marketBooks = bfApi.listMarketBook(marketIds, priceProjection, null, null, "USD", APP_KEY, sessionToken);
        if(!marketBooks.get(0).getInplay()) {
            LOG.warn("inPlay=false");
        }
        while (marketBooks.get(0).getInplay()) {
            for (Runner runner : marketBooks.get(0).getRunners()) {
                if(!"ACTIVE".equals(runner.getStatus())) {
                    continue;
                }
                ExchangePrices ex = runner.getEx();
                List<PriceSize> availableToLay = ex.getAvailableToLay();
                if(availableToLay.isEmpty()) {
                    continue;
                }
                if(availableToLay.get(0).getPrice() <= tradingConfig.getMinPrice()) {
                    return runner.getSelectionId();
                }
            }
            try {
                marketBooks = bfApi.listMarketBook(marketIds, priceProjection, null, null, "USD", APP_KEY, sessionToken);
            } catch (APINGException e) {
                if(!"TIMEOUT_ERROR".equals(e.getErrorCode())) {
                    throw e;
                }
            }
            if(!marketBooks.get(0).getInplay()) {
                LOG.warn("inPlay=false");
            }
        }
        return null;
    }

    private void updateTradingContext() throws Exception, APINGException {
        Double cycleBalance = tradingContext.getCycleBalance();
        Double betUnit = tradingContext.getBetUnit();
        Double betAmount = tradingContext.getBetAmount();
        List<TradingResult> tradingHistory = tradingContext.getTradingHistory();
        boolean isLastWin = false;
        if(!tradingHistory.isEmpty()) {
            TradingResult last = tradingHistory.get(tradingHistory.size() - 1);
            isLastWin = last.getProfit() > 0;
        }
        if(cycleBalance != 0d && cycleBalance < CalcUtils.round(betUnit * 0.9, 2)) {
            double times = betAmount / betUnit;
            if(times < 3.0d && isLastWin) {
                tradingContext.setBetAmount(CalcUtils.round(betAmount + betUnit, 2));
            }
        } else {
            tradingContext.setCycleBalance(0d);
            double newBetUnit = CalcUtils.round(getBalance() / 100d * tradingConfig.getStakeSize(), 2);
            newBetUnit = CalcUtils.round(newBetUnit / 93.5d * 100d, 2);
            tradingContext.setBetUnit(newBetUnit);
            tradingContext.setBetAmount(newBetUnit);
        }
    }
}
