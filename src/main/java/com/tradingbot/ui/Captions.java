package com.tradingbot.ui;

import java.io.Serializable;
import java.util.ListResourceBundle;

public class Captions extends ListResourceBundle implements Serializable {

    static int ids = 0;

    public static final String support = generateId();
    public static final String blankUsernameMsg = generateId();
    public static final String invalidUsernameMsg = generateId();
    public static final String subscriptionMsg = generateId();
    public static final String errorPrefixMsg = generateId();
    public static final String errorPostfixMsg = generateId();
    public static final String username = generateId();
    public static final String usernamePrompt = generateId();
    public static final String password = generateId();
    public static final String login = generateId();
    public static final String start = generateId();
    public static final String stop = generateId();
    public static final String botStarted = generateId();
    public static final String botStopped = generateId();
    public static final String subscriptionExpired = generateId();
    public static final String minBalance = generateId();
    public static final String activateTrial = generateId();
    public static final String trialActivated = generateId();
    public static final String eventsLog = generateId();
    public static final String today = generateId();
    public static final String last7days = generateId();
    public static final String last30days = generateId();
    public static final String welcome = generateId();
    public static final String balance = generateId();
    public static final String expireDate = generateId();
    public static final String botStatus = generateId();
    public static final String active = generateId();
    public static final String notActive = generateId();
    public static final String profit = generateId();
    public static final String getDiscount = generateId();
    public static final String yourDiscount = generateId();
    public static final String discountCode = generateId();
    public static final String discountExpires = generateId();
    public static final String tradingHistory = generateId();

    static final Object[][] contents = {
            {support, "Support"},
            {blankUsernameMsg, "Username or password cannot be blank"},
            {invalidUsernameMsg, "Invalid username or password"},
            {subscriptionMsg, "Please make sure that you have active GeeksToy subscription linked to your Betfair account"},
            {errorPrefixMsg, "An error occurred during login process"},
            {errorPostfixMsg, "Please contact support for help"},
            {username, "Username"},
            {usernamePrompt, "Your Betfair username"},
            {password, "Password"},
            {login, "Login"},
            {start, "Start"},
            {stop, "Stop"},
            {botStarted, "Bot started"},
            {botStopped, "Bot stopped"},
            {subscriptionExpired, "Subscription expired"},
            {minBalance, "Minimum required balance"},
            {activateTrial, "Activate trial"},
            {trialActivated, "Trial subscription activated"},
            {eventsLog, "Events log"},
            {today, "Today"},
            {last7days, "Last 7 days"},
            {last30days, "Last 30 days"},
            {welcome, "Welcome"},
            {balance, "Balance"},
            {expireDate, "Subscription expires"},
            {botStatus, "Bot status"},
            {active, "Active"},
            {notActive, "Inactive"},
            {profit, "Profit"},
            {getDiscount, "Get discount"},
            {yourDiscount, "<b>Your %s%% discount</b>"},
            {discountCode, "Discount code"},
            {discountExpires, "Discount expires"},
            {tradingHistory, "Trading history"},
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }

    public static String generateId() {
        return Integer.toString(ids++);
    }

}
