package com.cryptoalert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("CoinDCX candles endpoint returned HTTP " + code + " for pair " + pair);
        }

        String response;
        try (Scanner sc = new Scanner(conn.getInputStream()).useDelimiter("\\A")) {
            response = sc.hasNext() ? sc.next() : "";
        }

        JSONArray arr = new JSONArray(response);
        List<Double> closes = new ArrayList<>();

        for (int i = arr.length() - 1; i >= 0; i--) {
            JSONObject candle = arr.getJSONObject(i);
            closes.add(candle.getDouble("close"));
        }

        return closes;
    }
}
