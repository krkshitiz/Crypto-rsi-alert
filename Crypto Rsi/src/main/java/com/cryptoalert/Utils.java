package com.cryptoalert;

public class Utils {
    public static String sanitizeInterval(String requested) {
        // Binance intervals include: 1m,3m,5m,15m,30m,1h,4h,1d etc.
        // We accept the provided value; user should set config accordingly.
        return requested;
    }
}

