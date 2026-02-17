package com.cryptoalert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CoinDCXClient {
    private final int timeoutMs;

    public CoinDCXClient(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public List<String> getAllMarketPairs() throws Exception {
        String url = "https://api.coindcx.com/exchange/v1/markets_details";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("CoinDCX markets endpoint returned HTTP " + code);
        }

        String response;
        try (Scanner sc = new Scanner(conn.getInputStream()).useDelimiter("\\A")) {
            response = sc.hasNext() ? sc.next() : "";
        }

        JSONArray arr = new JSONArray(response);
        List<String> pairs = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);

            if (!obj.getString("status").equalsIgnoreCase("active")) {
                continue;
            }

            String symbol = obj.getString("symbol");
            String pair = obj.getString("pair");

            if (symbol.endsWith("USDT") || symbol.endsWith("INR")) {
                pairs.add(pair);
            }
        }

        return pairs;
    }
}
