package com.tradingbot.market.traders;

import com.betfair.aping.entities.*;
import com.betfair.aping.enums.*;
import com.betfair.aping.exceptions.APINGException;
import com.betfair.aping.util.SessionTokenDiscoverer;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tradingbot.entities.TradingConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tradingbot.BetfairUtils.APP_KEY;


public abstract class AbstractMarketTrader extends GenericTrader implements MarketTrader {

    private static final Logger LOG = Logger.getLogger(AbstractMarketTrader.class);
    protected final MarketCatalogue market;

    public AbstractMarketTrader(MarketCatalogue market, TradingConfig tradingConfig, SessionTokenDiscoverer tokenDiscoverer) {
        super(tradingConfig, tokenDiscoverer);
        this.market = market;
    }

    protected PlaceInstructionReport placeBet(String marketId, Long selectionId, Side side, double betAmount, double betPrice) throws APINGException, Exception {
        PlaceInstruction instruction = new PlaceInstruction();
        instruction.setOrderType(OrderType.LIMIT);
        LimitOrder limitOrder = new LimitOrder();
        limitOrder.setPersistenceType(PersistenceType.PERSIST);
        instruction.setLimitOrder(limitOrder);
        instruction.setSelectionId(selectionId);
        List<PlaceInstruction> placeInstructions = new ArrayList<>();
        placeInstructions.add(instruction);
        instruction.setSide(side);
        limitOrder.setPrice(betPrice);
        limitOrder.setSize(betAmount);
        PlaceExecutionReport placeBetResult = null;
        while (placeBetResult == null) {
            try {
                placeBetResult = bfApi.placeOrders(marketId, placeInstructions, null, APP_KEY, tokenDiscoverer.getSessionToken());
            } catch (APINGException e) {
                if(!"TIMEOUT_ERROR".equals(e.getErrorCode())) {
                    throw e;
                }
            }
        }
        List<PlaceInstructionReport> instructionReports = placeBetResult.getInstructionReports();
        if(placeBetResult.getErrorCode() != null) {
            LOG.warn("PlaceExecutionReport.errorCode="+placeBetResult.getErrorCode());
            if(instructionReports != null && !instructionReports.isEmpty()) {
                LOG.warn("PlaceInstructionReport.errorCode="+ instructionReports.get(0).getErrorCode());
                return instructionReports.get(0);
            }
        }
        if(instructionReports != null && !instructionReports.isEmpty()) {
            String betId = instructionReports.get(0).getBetId();
            InstructionReportStatus status = instructionReports.get(0).getStatus();
            LOG.debug("PlaceBetReport: status="+status+";"+placeBetResult.getStatus()+" exchangeBetId="+betId);
            return instructionReports.get(0);
        }
        return null;
    }

    protected ClearedOrderSummary getClearedOrderSummary(String marketId, String betId) throws Exception, APINGException {
        GroupBy groupBy = GroupBy.MARKET;
        Set<String> betIds = null;
        if(StringUtils.isNotBlank(betId)) {
            groupBy = GroupBy.BET;
            betIds = Sets.newHashSet(betId);
        }
        ClearedOrderSummary clearedOrder = getClearedOrderSummary(marketId, groupBy, betIds);
        while (clearedOrder == null) {
            TimeUnit.SECONDS.sleep(1);
            clearedOrder = getClearedOrderSummary(marketId, groupBy, betIds);
        }
        return clearedOrder;
    }

    private ClearedOrderSummary getClearedOrderSummary(String marketId, GroupBy groupBy, Set<String> betIds) throws Exception, APINGException {
        try {
            ClearedOrderSummaryReport ordersReport = bfApi.listClearedOrders(
                    BetStatus.SETTLED, null, null, Sets.newHashSet(marketId), null, betIds, null, null, groupBy,
                    false, null, 0, 100, APP_KEY, tokenDiscoverer.getSessionToken());
            List<ClearedOrderSummary> clearedOrders = ordersReport != null ? ordersReport.getClearedOrders() : null;
            if(clearedOrders != null && !clearedOrders.isEmpty()) {
                return clearedOrders.get(0);
            }
        } catch (SSLPeerUnverifiedException ignored) {
            LOG.warn("SSLPeerUnverifiedException");
        } catch (APINGException e) {
            if(!"TIMEOUT_ERROR".equals(e.getErrorCode())) {
                throw e;
            }
        }
        return null;
    }

    protected Long getWinner(String marketId) throws Exception, APINGException {
        ArrayList<String> marketIds = Lists.newArrayList(marketId);
        String sessionToken = tokenDiscoverer.getSessionToken();
        PriceProjection priceProjection = new PriceProjection();
        priceProjection.setPriceData(Sets.newHashSet(PriceData.EX_BEST_OFFERS));
        List<MarketBook> marketBooks = bfApi.listMarketBook(marketIds, priceProjection, null, null, "USD", APP_KEY, sessionToken);
        int suspendedCounter = 0;
        String status = marketBooks.get(0).getStatus();
        while (!"CLOSED".equals(status)) {
            status = marketBooks.get(0).getStatus();
            if("SUSPENDED".equals(status)) {
                TimeUnit.SECONDS.sleep(1);
                suspendedCounter++;
            } else {
                suspendedCounter = 0;
            }
            if(suspendedCounter > 9) {
                break;
            }
            try {
                marketBooks = bfApi.listMarketBook(marketIds, priceProjection, null, null, "USD", APP_KEY, sessionToken);
            } catch (APINGException e) {
                if(!"TIMEOUT_ERROR".equals(e.getErrorCode())) {
                    throw e;
                }
            }
        }
        return getLowestPriceRunner(marketBooks.get(0));
    }

    protected Long getLowestPriceRunner(MarketBook marketBook) {
        long result = 0;
        double minPrice = 1000d;
        for (Runner runner : marketBook.getRunners()) {
            Double price = runner.getLastPriceTraded();
            if(price == null) {
                continue;
            }
            if(minPrice > price) {
                minPrice = price;
                result = runner.getSelectionId();
            }
        }
        return result;
    }

    protected String getRunnerName(Long selectionId) {
        for (RunnerCatalog runnerCatalog : market.getRunners()) {
            if(runnerCatalog.getSelectionId().equals(selectionId)) {
                return runnerCatalog.getRunnerName();
            }
        }
        return "N/A";
    }
}
