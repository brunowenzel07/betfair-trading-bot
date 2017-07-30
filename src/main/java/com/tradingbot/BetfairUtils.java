package com.tradingbot;

import com.betfair.aping.api.ApiNgJsonRpcOperations;
import com.betfair.aping.api.ApiNgOperations;
import com.betfair.aping.entities.AccountDetailsResponse;
import com.betfair.aping.entities.AccountFundsResponse;
import com.betfair.aping.enums.Wallet;
import com.betfair.aping.exceptions.APINGException;
import com.betfair.aping.util.SessionTokenDiscoverer;
import com.tradingbot.entities.AccountDetails;
import org.apache.commons.lang3.StringUtils;

public class BetfairUtils {

    public static final String APP_KEY = "oeEBR9siPRhuCasu";
    public static final double MIN_BALANCE = 50;
    public static final double MIN_TOTAL_MATCHED = 50 * 1000;

    public static SessionTokenDiscoverer createTokenDiscoverer(String userName, String passWord) throws Exception, APINGException {
        SessionTokenDiscoverer discoverer = new SessionTokenDiscoverer(userName, passWord, APP_KEY);
        String sessionToken = discoverer.getSessionToken();
        if(StringUtils.isNotBlank(sessionToken)) {
            return discoverer;
        }
        throw new Exception("Unable to create token discoverer.");
    }

    public static AccountDetails getAccountDetails(SessionTokenDiscoverer tokenDiscoverer) throws Exception, APINGException {
        String sessionToken = tokenDiscoverer.getSessionToken();
        ApiNgOperations bfApi = ApiNgJsonRpcOperations.createNewInstance();
        AccountFundsResponse accountFunds = bfApi.getAccountFunds(Wallet.UK, APP_KEY, sessionToken);
        AccountDetailsResponse accountDetails = bfApi.getAccountDetails(APP_KEY, sessionToken);
        String currency = accountDetails.getCurrencyCode();
        Double balance = accountFunds.getAvailableToBetBalance();
        String fullName = accountDetails.getFirstName() + " " + accountDetails.getLastName();
        return new AccountDetails(balance, currency, fullName);
    }
}
