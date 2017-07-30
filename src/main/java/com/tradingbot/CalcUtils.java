package com.tradingbot;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class CalcUtils {

    public static double round(double value, int places) {
        String format = "#.";
        for (int i = 0; i < places; i++) {
            format+="#";
        }
        DecimalFormat df = new DecimalFormat(format);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(symbols);
        return Double.valueOf(df.format(value));
    }

    public static double layToBackPrice(double layPrice) {
        return round((((layPrice / (layPrice - 1)) - 1) * 0.935) + 1, 3);
    }

    public static double calculateSureBet(double firstPrice, double secondPrice) {
        double p = (1 / firstPrice) + (1 / secondPrice);
        return round(100 / p - 100, 2);
    }

    public static double calculateLayStakeSize(double layPrice, double liabilitySize) {
        return round(liabilitySize / (layPrice - 1), 2);
    }

    public static double calculateLiabilitySize(double layPrice, double layAmount) {
        return round((layPrice - 1) * layAmount , 2);
    }

    public static double adjustLayPrice(double exchangeBetSize, double maxLiability) {
        return round((maxLiability / exchangeBetSize) + 1, 2);
    }

    public static double calculateProfit(double betSize, double betPrice) {
        return round(betSize * (betPrice - 1), 2);
    }

    public static double netOfCommission(double price) {
        return round(((price - 1) * 0.935) + 1, 3);
    }

    /*returns size with valid increments*/
    public static double validateExchangePrice(double price, boolean reduce) {
        if(price < 2) {
            return price;
        }
        int incr = 1;
        if(price > 2 && price < 3) {
            incr = 2;
        }
        if(price > 3 && price < 4) {
            incr = 5;
        }
        if(price > 4 && price < 6) {
            incr = 10;
        }
        if(price > 6 && price < 10) {
            incr = 20;
        }
        if(price > 10 && price < 20) {
            incr = 50;
        }
        if(price > 20 && price < 30) {
            incr = 100;
        }
        int iPrice = (int) (price * 100d);
        int remainder = iPrice % incr;
        if(reduce) {
            return round((iPrice - remainder) / 100d, 2);
        }
        return round((iPrice + (incr - remainder)) / 100d, 2);
    }

    public static double increasePrice(double price, int times) {
        double incrementedPrice = price;
        for (int i = 0; i < times; i++) {
            double increment = 0.01;
            if(incrementedPrice >= 2 && incrementedPrice < 3) {
                increment = 0.02;
            }
            if(incrementedPrice >= 3 && incrementedPrice < 4) {
                increment = 0.05;
            }
            if(incrementedPrice >= 4 && incrementedPrice < 6) {
                increment = 0.1;
            }
            if(incrementedPrice >= 6 && incrementedPrice < 10) {
                increment = 0.2;
            }
            if(incrementedPrice >= 10 && incrementedPrice < 20) {
                increment = 0.5;
            }
            if(incrementedPrice >= 20 && incrementedPrice < 30) {
                increment = 1;
            }
            incrementedPrice = round(incrementedPrice + increment, 2);
        }
        return round(incrementedPrice, 2);
    }
}
