package com.tradingbot.market.traders;

import com.betfair.aping.api.ApiNgJsonRpcOperations;
import com.betfair.aping.api.ApiNgOperations;
import com.betfair.aping.entities.*;
import com.betfair.aping.enums.OrderBy;
import com.betfair.aping.enums.OrderProjection;
import com.betfair.aping.enums.PriceData;
import com.betfair.aping.enums.Wallet;
import com.betfair.aping.exceptions.APINGException;
import com.betfair.aping.util.SessionTokenDiscoverer;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tradingbot.MessageListener;
import com.tradingbot.entities.TradingContext;
import com.tradingbot.entities.TradingConfig;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tradingbot.BetfairUtils.APP_KEY;

public class GenericTrader {

    private static final Logger LOG = Logger.getLogger(GenericTrader.class);
    protected final ApiNgOperations bfApi = ApiNgJsonRpcOperations.createNewInstance();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    protected MessageListener messageListener = null;
    protected TradingConfig tradingConfig;
    protected TradingContext tradingContext;
    protected SessionTokenDiscoverer tokenDiscoverer;

    public GenericTrader(TradingConfig tradingConfig, SessionTokenDiscoverer tokenDiscoverer) {
        this.tradingConfig = tradingConfig;
        this.tokenDiscoverer = tokenDiscoverer;
    }

    protected List<Runner> getActiveRunners(String marketId) throws Exception, APINGException {
        List<Runner> result = new ArrayList<>();
        PriceProjection priceProjection = new PriceProjection();
        priceProjection.setPriceData(Sets.newHashSet(PriceData.EX_BEST_OFFERS));
        List<MarketBook> marketBookList = bfApi.listMarketBook(Lists.newArrayList(marketId), priceProjection, null, null, "USD", APP_KEY, tokenDiscoverer.getSessionToken());
        MarketBook marketBook = marketBookList.get(0);
        for (Runner runner : marketBook.getRunners()) {
            if("ACTIVE".equals(runner.getStatus())) {
                result.add(runner);
            }
        }
        return result;
    }

    protected void waitForInplay(String marketId) throws Exception, APINGException {
        ArrayList<String> marketIds = Lists.newArrayList(marketId);
        String sessionToken = tokenDiscoverer.getSessionToken();
        List<MarketBook> marketBooks = bfApi.listMarketBook(marketIds, null, null, null, "USD", APP_KEY, sessionToken);
        while (marketBooks.isEmpty() || !marketBooks.get(0).getInplay()) {
            TimeUnit.SECONDS.sleep(1);
            try {
                marketBooks = bfApi.listMarketBook(marketIds, null, null, null, "USD", APP_KEY, sessionToken);
            } catch (APINGException e) {
                if(!"TIMEOUT_ERROR".equals(e.getErrorCode())) {
                    throw e;
                }
            }
        }
    }

    protected String getDistance(MarketCatalogue marketCatalogue) {
        return marketCatalogue.getMarketName().split(" ")[0].toLowerCase().trim();
    }

    protected CurrentOrderSummary waitForFullyMatched(String betId) throws Exception, APINGException {
        CurrentOrderSummary orderSummary = new CurrentOrderSummary();
        orderSummary.setSizeRemaining(1);
        while (orderSummary.getSizeRemaining() > 0d) {
            try {
                orderSummary = getOrderSummary(betId);
            } catch (APINGException e) {
                if(!"TIMEOUT_ERROR".equals(e.getErrorCode())) {
                    throw e;
                }
            }
        }
        return orderSummary;
    }

    protected CurrentOrderSummary getOrderSummary(String betId) throws Exception, APINGException {
        CurrentOrderSummaryReport orders = bfApi.listCurrentOrders(Sets.newHashSet(betId), null,
                OrderProjection.ALL, null, null, OrderBy.BY_PLACE_TIME, null, 0, 100, APP_KEY, tokenDiscoverer.getSessionToken());
        while (orders.getCurrentOrders().isEmpty()) {
            LOG.warn("orders.getCurrentOrders().isEmpty() = true; betId="+betId);
            orders = bfApi.listCurrentOrders(Sets.newHashSet(betId), null,
                    OrderProjection.ALL, null, null, OrderBy.BY_PLACE_TIME, null, 0, 100, APP_KEY, tokenDiscoverer.getSessionToken());
        }
        return orders.getCurrentOrders().get(0);
    }

    protected double getBalance() throws Exception, APINGException {
        AccountFundsResponse accountFunds = bfApi.getAccountFunds(Wallet.UK, APP_KEY, tokenDiscoverer.getSessionToken());
        return accountFunds.getAvailableToBetBalance();
    }

    protected String getEventName(MarketCatalogue market) {
        return market.getEvent().getName() + " "+timeFormat.format(market.getMarketStartTime())+" "+market.getMarketName();
    }

    protected void logMessage(String message) {
        LOG.info(message);
        if(messageListener != null) {
            messageListener.newMessage(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " - " + message);
        }
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void setTradingConfig(TradingConfig tradingConfig) {
        this.tradingConfig = tradingConfig;
    }

    public void setTokenDiscoverer(SessionTokenDiscoverer tokenDiscoverer) {
        this.tokenDiscoverer = tokenDiscoverer;
    }

    public TradingContext getTradingContext() {
        return tradingContext;
    }

    public void setTradingContext(TradingContext tradingContext) {
        this.tradingContext = tradingContext;
    }
}
