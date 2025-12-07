package com.cryptoalert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Scanner;

public class CoinDCXCandleClient {
    private final int timeoutMs;

    public CoinDCXCandleClient(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public List<Double> getClosePrices(String pair, String interval, int limit) throws Exception {
        String url = "https://public.coindcx.com/market_data/candles"
                + "?pair=" + pair
                + "&interval=" + interval
                + "&limit=" + limit;
        
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        
        Scanner sc = new Scanner(conn.getInputStream());
        StringBuilder sb = new StringBuilder();
        while (sc.hasNext()) sb.append(sc.nextLine());
        sc.close();
        
        JSONArray arr = new JSONArray(sb.toString());
        List<Double> closes = new ArrayList<>();

        // API returns newest → oldest, we reverse because RSI needs oldest→newest
        // Each candle is a JSONObject with fields: open, high, low, volume, close, time
        for (int i = arr.length() - 1; i >= 0; i--) {
            JSONObject candle = arr.getJSONObject(i);
            closes.add(candle.getDouble("close")); // close price
        }

        return closes;
    }
}

