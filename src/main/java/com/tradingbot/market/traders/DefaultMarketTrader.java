package com.tradingbot.market.traders;

import com.betfair.aping.entities.*;
import com.betfair.aping.enums.*;
import com.betfair.aping.exceptions.APINGException;
import com.betfair.aping.util.SessionTokenDiscoverer;
import com.tradingbot.CalcUtils;
import com.tradingbot.entities.TradingResult;
import com.tradingbot.entities.TradingConfig;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class DefaultMarketTrader extends AbstractMarketTrader {

    public static final String NAME = "default";
    private static final Logger LOG = Logger.getLogger(DefaultMarketTrader.class);
    private static final Map<String , Integer> distanceRunnerCountMap = new HashMap<String , Integer>(){{
        put("1m3f", 9);
        put("1m4f", 9);
        put("1m5f", 9);
        put("1m6f", 9);
        put("1m7f", 9);
        put("1m8f", 9);
        put("1m9f", 9);
        put("2m", 8);
        put("2m1f", 8);
        put("2m2f", 8);
        put("2m3f", 7);
        put("2m4f", 7);
        put("2m5f", 7);
        put("2m6f", 7);
        put("2m7f", 7);
        put("2m8f", 7);
        put("2m9f", 7);
        put("3m", 6);
        put("3m1f", 6);
        put("3m2f", 6);
        put("3m3f", 6);
        put("3m4f", 6);
        put("3m5f", 6);
        put("3m6f", 6);
        put("3m7f", 6);
        put("3m8f", 6);
    }};

    private final String marketId;
    private final double exchangeFee = 6.5;

    public DefaultMarketTrader(MarketCatalogue market, TradingConfig tradingConfig, SessionTokenDiscoverer discoverer) {
        super(market, tradingConfig, discoverer);
        this.marketId = market.getMarketId();
    }

    public TradingResult trade() throws Exception, APINGException {
        String eventName = getEventName(market);
        if(tradingConfig.isInplay()) {
            logMessage(String.format("Waiting for in play '%s'", eventName));
            waitForInplay(market.getMarketId());
            logMessage(String.format("In play started '%s'", eventName));
        }
        List<Runner> activeRunners = getActiveRunners(marketId);
        int activeRunnersCount = activeRunners.size();
        String distance = getDistance(market);
        Integer minRunnerCount = distanceRunnerCountMap.get(distance);
        if(minRunnerCount == null) {
            LOG.debug(String.format("Mapped runner count for distance '%s' not found", distance));
            return null;
        }
        if(!(activeRunnersCount > minRunnerCount)) {
            logMessage("Market is not suitable for trading.");
            LOG.debug(String.format("Market is not suitable by active runners count=%d; distance %s", activeRunnersCount, distance));
            return null;
        }
        Runner runner = findSuitableRunner(activeRunners);
        if(runner == null) {
            logMessage("Suitable runner was not found.");
            return null;
        }
        LOG.debug(String.format("Suitable runner %s was found with price %s", getRunnerName(runner.getSelectionId()), runner.getLastPriceTraded()));
        LOG.debug(String.format("Execute placing bets on marketId %s selectionId %d ...", marketId, runner.getSelectionId()));
        logMessage("Execute placing bets...");
        PlaceInstructionReport layBetReport = placeLayBet(marketId, runner);
        if(layBetReport == null || !layBetReport.getStatus().equals(InstructionReportStatus.SUCCESS)) {
            LOG.error("Failed to place LAY bet.");
            return null;
        }
        logMessage(String.format("LAY bet placed successfully: size %s; avgPrice %s", layBetReport.getSizeMatched(), layBetReport.getAveragePriceMatched()));
        PlaceInstructionReport backBetReport = placeBackBet(layBetReport, marketId, runner);
        if(backBetReport == null || !backBetReport.getStatus().equals(InstructionReportStatus.SUCCESS)) {
            LOG.debug("Failed to place BACK bet.");
            return null;
        }
        logMessage(String.format("BACK bet placed successfully: size %s; price %s", backBetReport.getSizeMatched(), backBetReport.getAveragePriceMatched()));
        TimeUnit.MINUTES.sleep(1);
        LOG.debug("Resolving order summary...");
        ClearedOrderSummary marketSummary = getClearedOrderSummary(marketId);
        ClearedOrderSummary layBetSummary = getClearedOrderSummary(marketId, layBetReport.getBetId());
        ClearedOrderSummary backBetSummary = getClearedOrderSummary(marketId, backBetReport.getBetId());
        double profit = CalcUtils.round(marketSummary.getProfit() - marketSummary.getCommission(), 2);
        String runnerName = getRunnerName(runner.getSelectionId());
        return new TradingResult(profit, runnerName,
                layBetSummary.getSizeSettled(), layBetSummary.getPriceMatched(),
                backBetSummary.getSizeSettled(), backBetSummary.getPriceMatched());
    }

    private ClearedOrderSummary getClearedOrderSummary(String marketId) throws Exception, APINGException {
        return getClearedOrderSummary(marketId, null);
    }

    private PlaceInstructionReport placeBackBet(PlaceInstructionReport layBetReport, String marketId, Runner runner) throws APINGException, Exception {
        double layBetAmount = layBetReport.getSizeMatched();
        double layBetPrice = layBetReport.getAveragePriceMatched();
        double backBetAmount = calculateBackBetAmount(layBetAmount);
        double backBetPrice = calculateBackBetPrice(layBetPrice, layBetAmount, backBetAmount);
        backBetAmount = adjustBackBetAmount(layBetAmount, layBetPrice, backBetPrice); //green up
        LOG.debug(String.format("Placing BACK bet: marketId %s; selectionId %d; amount %s; price %s ...",
                                                    marketId, runner.getSelectionId(), backBetAmount, backBetPrice));
        return placeBet(marketId, runner.getSelectionId(), Side.BACK, backBetAmount, backBetPrice);
    }

    private double adjustBackBetAmount(double layBetAmount, double layBetPrice, double backBetPrice) {
        return CalcUtils.round((layBetAmount * layBetPrice) / backBetPrice, 2);
    }

    private double calculateBackBetPrice(double layBetPrice, double layBetAmount, double backBetAmount) {
        double liabilitySize = CalcUtils.calculateLiabilitySize(layBetPrice, layBetAmount);
        double layBetProfit = calculateProfitAmount(layBetAmount, true);
        double backBetPrice = CalcUtils.validateExchangePrice(layBetPrice, true);
        double backBetProfit;
        do{
            backBetPrice = CalcUtils.increasePrice(backBetPrice, 1);
            backBetProfit = CalcUtils.calculateProfit(backBetAmount, backBetPrice) - liabilitySize;
            double netOfFeeRatio = CalcUtils.round(1 - (exchangeFee / 100), 4);
            backBetProfit = CalcUtils.round(backBetProfit * netOfFeeRatio, 2);
        } while (backBetProfit < layBetProfit);
        return backBetPrice;
    }

    private double calculateBackBetAmount(double layBetAmount) {
        double profitAmount = calculateProfitAmount(layBetAmount, false);
        return CalcUtils.round(layBetAmount - profitAmount, 2);
    }

    private double calculateProfitAmount(double layBetAmount, boolean netOfCommission) {
        double profitAmount = layBetAmount / 100 * tradingConfig.getProfitSize();
        profitAmount = CalcUtils.round(profitAmount, 2);
        if(netOfCommission) {
            return profitAmount;
        }
        return CalcUtils.round((profitAmount / (100 - exchangeFee) * 100), 2);
    }

    private PlaceInstructionReport placeLayBet(String marketId, Runner runner) throws Exception, APINGException {
        double betAmount = calculateBetAmount();
        Double layPrice = getLayPrice(runner);
        Double betPrice = CalcUtils.validateExchangePrice(layPrice + 1, true);
        if(betPrice > tradingConfig.getMaxPrice()) {
            betPrice = tradingConfig.getMaxPrice();
        }
        LOG.debug(String.format("Placing LAY bet: marketId %s; selectionId %d; amount %s; price %s ...",
                                                    marketId, runner.getSelectionId(), betAmount, layPrice));
        PlaceInstructionReport report = placeBet(marketId, runner.getSelectionId(), Side.LAY, betAmount, betPrice);
        boolean partiallyMatched = betAmount > report.getSizeMatched();
        boolean successfullyPlaced = InstructionReportStatus.SUCCESS.equals(report.getStatus());
        if(successfullyPlaced && partiallyMatched) {
            LOG.debug(String.format("Waiting for matching bet %s ...", report.getBetId()));
            CurrentOrderSummary orderSummary = waitForFullyMatched(report.getBetId());
            report.setSizeMatched(orderSummary.getSizeMatched());
            report.setAveragePriceMatched(orderSummary.getAveragePriceMatched());
        }
        return report;
    }

    private double calculateBetAmount() throws Exception, APINGException {
        Double availableToBetBalance = getBalance();
        LOG.debug("Balance: "+availableToBetBalance);
        double betAmount = availableToBetBalance / 100 * tradingConfig.getStakeSize();
        return CalcUtils.round(betAmount, 2);
    }

    private Double getLayPrice(Runner runner) {
        ExchangePrices ex = runner.getEx();
        List<PriceSize> availableToLay = ex.getAvailableToLay();
        if(availableToLay.isEmpty()) {
            return 1000d;
        }
        PriceSize bestLayOffer = availableToLay.get(0);
        return bestLayOffer.getPrice();
    }

    private Runner findSuitableRunner(List<Runner> activeRunners) {
        Runner result = null;
        double minPrice = 1000;
        List<Runner> filteredByPrice = filterRunnersByPrice(activeRunners);
        for (Runner runner : filteredByPrice) {
            Double runnerPrice = getLayPrice(runner);
            if(runnerPrice < minPrice) {
                result  = runner;
                minPrice = runnerPrice;
            }
        }
        return result;
    }

    private List<Runner> filterRunnersByPrice(List<Runner> activeRunners) {
        List<Runner> result = new ArrayList<>();
        for (Runner runner : activeRunners) {
            Double runnerPrice = getLayPrice(runner);
            if(runnerPrice > tradingConfig.getMinPrice() &&
                    runnerPrice < tradingConfig.getMaxPrice()) {
                result.add(runner);
            }
        }
        return result;
    }

}
