package com.tradingbot;

import com.betfair.aping.entities.*;
import com.betfair.aping.enums.MarketProjection;
import com.betfair.aping.enums.MarketSort;
import com.betfair.aping.exceptions.APINGException;
import com.betfair.aping.util.SessionTokenDiscoverer;
import com.google.common.collect.Sets;
import com.tradingbot.entities.*;
import com.tradingbot.market.traders.AbstractMarketTrader;
import com.tradingbot.market.traders.DefaultMarketTrader;
import com.tradingbot.market.traders.GenericTrader;
import com.tradingbot.market.traders.OscarMarketTrader;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.tradingbot.BetfairUtils.APP_KEY;
import static com.tradingbot.BetfairUtils.MIN_BALANCE;
import static com.tradingbot.BetfairUtils.MIN_TOTAL_MATCHED;

public class TradingEngine extends GenericTrader {

    private static final Logger LOG = Logger.getLogger(TradingEngine.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final List<Future> tasks = new ArrayList<>();
    private User user;
    private TradingContext tradingContext;

    public TradingEngine(User user, SessionTokenDiscoverer tokenDiscoverer) {
        super(user.getTradingConfig(), tokenDiscoverer);
        this.user = user;
    }

    public void start() {
        checkSubscriptionExpiration();
        if(tasks.isEmpty()) {
            tasks.add(executorService.submit(createMainTask()));
        }
    }

    public void stop() {
        for (Future task : tasks) {
            task.cancel(true);
        }
        tasks.clear();
    }

    public boolean isRunning(){
        return !tasks.isEmpty();
    }

    private Runnable createMainTask() {
        return () -> {
            Thread.currentThread().setName(user.getUserName()+" => mainTask");
            try {
                logMessage("Trading engine started.");
                MarketCatalogue suitableMarket = getSuitableMarket();
                if(tradingContext == null) {
                    tradingContext = createTradingContext();
                    LOG.debug("Trading context created.");
                }
                while (suitableMarket != null) {
                    checkSubscriptionExpiration();
                    String eventName = getEventName(suitableMarket);
                    logMessage(String.format("Waiting event '%s' for start...", eventName));
                    waitForCountdown(suitableMarket.getMarketStartTime(), user.getTradingConfig().getCountdownSeconds());
                    if(getBalance() < MIN_BALANCE) {
                        logMessage("Insufficient balance.");
                        break;
                    }
                    if(getTotalMatched(suitableMarket) > MIN_TOTAL_MATCHED) {
                        executorService.submit(createMarketTask(suitableMarket));
                    }
                    TimeUnit.SECONDS.sleep(20);
                    suitableMarket = getSuitableMarket();
                }
            } catch (SubscriptionExpiredException e) {
                logMessage("Subscription expired.");
            } catch (InterruptedException e) {
                //stopped manually by user
            } catch (APINGException | Exception e) {
                LOG.error("Main task stopped due to error: "+e.getMessage(), e);
            }
            tasks.clear();
            logMessage("Trading engine stopped.");
        };
    }

    private double getTotalMatched(MarketCatalogue market) throws Exception, APINGException {
        String marketId = market.getMarketId();
        MarketFilter marketFilter = new MarketFilter();
        marketFilter.setMarketIds(Sets.newHashSet(marketId));
        String sessionToken = tokenDiscoverer.getSessionToken();
        List<MarketCatalogue> markets = bfApi.listMarketCatalogue(marketFilter, null, null, "1", APP_KEY, sessionToken);
        if(!markets.isEmpty()) {
            return markets.get(0).getTotalMatched();
        } else {
            LOG.error(String.format("Market not found by marketId='%s'", marketId));
            return 0;
        }
    }

    private TradingContext createTradingContext() throws Exception, APINGException {
        TradingContext tradingContext = new TradingContext();
        double betUnit = CalcUtils.round(getBalance() / 100 * tradingConfig.getStakeSize(), 2);
        tradingContext.setBetUnit(betUnit);
        tradingContext.setBetAmount(betUnit);
        List<TradingResult> tradingResults = new ArrayList<>();
        DBHelper.updateUser(user.getUserName(), user -> {
            List<BetReport> betReports = new ArrayList<>(user.getBetReports());
            betReports.sort((o1, o2) -> (int) (o1.getStartTime().getTime() - o2.getStartTime().getTime()));
            for (BetReport betReport : betReports) {
                if(betReport.getBackPrice() == null) {
                    tradingResults.add(new TradingResult(betReport.getProfit()));
                }
            }
        });
        tradingContext.setTradingHistory(tradingResults);
        return tradingContext;
    }

    private Runnable createMarketTask(final MarketCatalogue market) {
        return () -> {
            String eventName = getEventName(market);
            Thread.currentThread().setName(user.getUserName()+" => "+eventName);
            AbstractMarketTrader marketTrader = createMarketTrader(tradingConfig, market);
            marketTrader.setMessageListener(messageListener);
            marketTrader.setTradingContext(tradingContext);
            try {
                TradingResult tradingResult = marketTrader.trade();
                if(tradingResult != null &&
                        (tradingResult.getProfit() > 0 || marketTrader instanceof OscarMarketTrader)) {
                    logMessage(String.format("%s profit fixed at '%s'", tradingResult.getProfit(), eventName));
                    addBetReport(market, tradingResult);
                }
            } catch (Exception | APINGException e) {
                LOG.error("An error occurred while trading event", e);
            }
        };
    }

    private AbstractMarketTrader createMarketTrader(TradingConfig tradingConfig, MarketCatalogue market) {
        AbstractMarketTrader marketTrader = new DefaultMarketTrader(market, user.getTradingConfig(), tokenDiscoverer);
        if(OscarMarketTrader.NAME.equals(tradingConfig.getTradingEngine())) {
            marketTrader = new OscarMarketTrader(market, user.getTradingConfig(), tokenDiscoverer);
        }
        return marketTrader;
    }

    private void addBetReport(MarketCatalogue market, TradingResult tradingResult) {
        DBHelper.updateUser(user.getUserName(), user -> {
            List<BetReport> betReports = user.getBetReports();
            if(betReports == null) {
                betReports = new ArrayList<>();
                user.setBetReports(betReports);
            }
            String marketName = market.getEvent().getName().split(" ")[0] + " : " + market.getMarketName();
            Date startTime = market.getMarketStartTime();
            betReports.add(new BetReport(marketName, startTime, tradingResult.getProfit(), tradingResult.getSelection(),
                    tradingResult.getLaySize(), tradingResult.getLayPrice(), tradingResult.getBackSize(), tradingResult.getBackPrice()));
        });
    }

    private void waitForCountdown(Date marketStartTime, long countdownSeconds) throws InterruptedException {
        long secondsToStart;
        do{
            checkSubscriptionExpiration();
            TimeUnit.SECONDS.sleep(1);
            secondsToStart = (marketStartTime.getTime() - System.currentTimeMillis()) / 1000L;
        } while (secondsToStart > countdownSeconds);
    }

    private void checkSubscriptionExpiration() {
        User user = DBHelper.getUser(this.user.getUserName());
        Date expire = this.user.getExpiryDate();
        if(user != null) {
            expire = user.getExpiryDate();
        } else {
            LOG.warn(String.format("Unable to load user by username '%s'", this.user.getUserName()));
        }
        if(System.currentTimeMillis() > expire.getTime()) {
            throw new SubscriptionExpiredException();
        }
    }

    private MarketCatalogue getSuitableMarket() throws APINGException, Exception {
        MarketFilter marketFilter = new MarketFilter();
        String sessionToken = tokenDiscoverer.getSessionToken();
        TimeRange time = new TimeRange();
        time.setFrom(new Date());
        marketFilter.setMarketStartTime(time);
        marketFilter.setMarketCountries(Sets.newHashSet("GB", "IE"));
        marketFilter.setMarketTypeCodes(Sets.newHashSet("WIN"));
        marketFilter.setEventTypeIds(Sets.newHashSet("7"));
        Set<MarketProjection> marketProjection = new HashSet<>();
        marketProjection.add(MarketProjection.RUNNER_DESCRIPTION);
        marketProjection.add(MarketProjection.EVENT);
        marketProjection.add(MarketProjection.MARKET_START_TIME);
        List<MarketCatalogue> marketCatalogueResult = bfApi.listMarketCatalogue(
                marketFilter, marketProjection, MarketSort.FIRST_TO_START, "50", APP_KEY, sessionToken);
        for (MarketCatalogue marketCatalogue : marketCatalogueResult) {
            if(isMarketSuitableForTrading(marketCatalogue)) {
                return marketCatalogue;
            }
        }
        return null;
    }

    private boolean isMarketSuitableForTrading(MarketCatalogue marketCatalogue) throws Exception, APINGException {
        String distance = getDistance(marketCatalogue);
        return isDistanceSuitable(distance);
    }

    private boolean isDistanceSuitable(String distance) {
        return distance.contains("3m") || distance.contains("2m") || distance.contains("1m");
    }

    public void setUser(User user) {
        this.user = user;
    }
}