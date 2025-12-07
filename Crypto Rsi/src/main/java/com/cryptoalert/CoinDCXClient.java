package com.cryptoalert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Scanner;

public class CoinDCXClient {
    private final int timeoutMs;

    public CoinDCXClient(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public List<String> getAllMarketPairs() throws Exception {
        String url = "https://api.coindcx.com/exchange/v1/markets_details";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);

        Scanner sc = new Scanner(conn.getInputStream());
        StringBuilder sb = new StringBuilder();
        while (sc.hasNext()) sb.append(sc.nextLine());
        sc.close();

        JSONArray arr = new JSONArray(sb.toString());
        List<String> pairs = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);

            if (!obj.getString("status").equalsIgnoreCase("active"))
                continue;

            String symbol = obj.getString("symbol"); // e.g., BTCUSDT
            String pair = obj.getString("pair");     // e.g., B-BTC_USDT (required for candles)

            // Track all markets with candle support (USDT and INR)
            if (symbol.endsWith("USDT") || symbol.endsWith("INR")) {
                pairs.add(pair);  // MUST use "pair", not "symbol"
            }
        }

        return pairs;
    }
}

