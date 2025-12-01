package com.cryptoalert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BinanceClient {
    private final int timeoutMs;
    public BinanceClient(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public List<String> getAllUSDTsymbols() throws Exception {
        String endpoint = "https://api.binance.com/api/v3/ticker/price";
        String resp = get(endpoint);
        JSONArray arr = new JSONArray(resp);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String sym = obj.getString("symbol");
            if (sym.endsWith("USDT")) result.add(sym);
        }
        return result;
    }

    public List<Double> getClosePrices(String symbol, String interval, int limit) throws Exception {
        // interval example: "5m", "1h"
        String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=" + limit;
        String resp = get(url);
        JSONArray arr = new JSONArray(resp);
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONArray k = arr.getJSONArray(i);
            double close = k.getDouble(4);
            closes.add(close);
        }
        return closes;
    }

    private String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("HTTP " + code + " for " + urlStr);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();
        return sb.toString();
    }
}

