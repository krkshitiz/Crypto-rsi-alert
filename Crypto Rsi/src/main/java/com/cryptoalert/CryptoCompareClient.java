package com.cryptoalert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CryptoCompareClient {
    private final int timeoutMs;
    private final String apiKey;

    public CryptoCompareClient(int timeoutMs, String apiKey) {
        this.timeoutMs = timeoutMs;
        this.apiKey = apiKey;
    }

    public List<String> getAllSymbols() throws Exception {
        // Fetch coin list and return coins that can be paired with USDT
        String endpoint = "https://min-api.cryptocompare.com/data/all/coinlist";
        String resp = get(endpoint);
        JSONObject obj = new JSONObject(resp);
        JSONObject data = obj.getJSONObject("Data");
        List<String> result = new ArrayList<>();
        for (String key : data.keySet()) {
            JSONObject coin = data.getJSONObject(key);
            // we will attempt to use TSYM=USDT for most coins; return symbol keys (e.g., BTC, ETH)
            result.add(key);
        }
        return result;
    }

    public List<Double> getClosePrices(String fsym, String tsym, int limit) throws Exception {
        // Use CryptoCompare historical 4-hour endpoint
        String url = String.format("https://min-api.cryptocompare.com/data/v2/histominute?fsym=%s&tsym=%s&limit=%d", fsym, tsym, limit-1);
        String resp = getWithApiKey(url);
        JSONObject root = new JSONObject(resp);
        if (!root.has("Data")) return new ArrayList<>();
        JSONObject data = root.getJSONObject("Data");
        if (!data.has("Data")) return new ArrayList<>();
        JSONArray arr = data.getJSONArray("Data");
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject k = arr.getJSONObject(i);
            double close = k.getDouble("close");
            closes.add(close);
        }
        return closes;
    }

    public List<Double> get5MinClosePrices(String fsym, String tsym, int limit5m) throws Exception {
    int needed1m = limit5m * 5;

    String url = String.format(
            "https://min-api.cryptocompare.com/data/v2/histominute?fsym=%s&tsym=%s&limit=%d",
            fsym, tsym, needed1m
    );

    String resp = getWithApiKey(url);

    JSONObject root = new JSONObject(resp);
    if (!root.has("Data")) return new ArrayList<>();

    JSONObject data = root.getJSONObject("Data");
    if (!data.has("Data")) return new ArrayList<>();

    JSONArray arr = data.getJSONArray("Data");

    List<Double> closes = new ArrayList<>();

    // compress into 5-minute candles
    for (int i = 0; i < arr.length(); i += 5) {
        JSONObject candle = arr.getJSONObject(i + 4); // last candle of the block
        closes.add(candle.getDouble("close"));
    }

    return closes;
}


    private String getWithApiKey(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("authorization", "Apikey " + apiKey);
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

