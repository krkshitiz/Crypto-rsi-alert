package com.cryptoalert;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws Exception {
        Properties cfg = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            cfg.load(in);
        } catch (Exception ignored) {
            System.out.println("config.properties not found, trying environment variables.");
        }

        String gmailUser = require(cfg, "gmail.username");
        String gmailAppPassword = require(cfg, "gmail.appPassword");
        String recipient = optional(cfg, "alert.recipient", gmailUser);
        int rsiPeriod = Integer.parseInt(require(cfg, "rsi.period"));
        double threshold = Double.parseDouble(require(cfg, "rsi.threshold"));
        int freqMin = Integer.parseInt(require(cfg, "scan.frequency.minutes"));
        int timeout = Integer.parseInt(require(cfg, "request.timeout.ms"));
        int limit = Integer.parseInt(require(cfg, "limit.candles"));
        String interval = require(cfg, "interval");

        CoinDCXClient dcx = new CoinDCXClient(timeout);
        CoinDCXCandleClient candleClient = new CoinDCXCandleClient(timeout);
        EmailNotifier emailNotifier = new EmailNotifier(gmailUser, gmailAppPassword, recipient);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        AtomicInteger lastProcessed = new AtomicInteger();
        AtomicInteger lastAlerts = new AtomicInteger();
        AtomicInteger lastFailed = new AtomicInteger();

        Runnable job = () -> {
            try {
                System.out.println("[" + LocalDateTime.now() + "] Starting full CoinDCX scan...");

                List<String> markets = dcx.getAllMarketPairs();
                System.out.println("Found " + markets.size() + " active markets.");

                int processed = 0;
                int failed = 0;
                int alerts = 0;

                for (String pair : markets) {
                    processed++;
                    try {
                        List<Double> closes = candleClient.getClosePrices(pair, interval, limit);
                        double rsi = RsiCalculator.computeRsi(closes, rsiPeriod);

                        if (rsi > threshold) {
                            alerts++;
                            String subject = "RSI ALERT: " + pair + " RSI=" + String.format("%.2f", rsi);
                            String body = "Pair: " + pair
                                    + "\nRSI: " + String.format("%.2f", rsi)
                                    + "\nThreshold: " + threshold
                                    + "\nTime: " + LocalDateTime.now();

                            emailNotifier.sendEmail(subject, body);
                            System.out.println("ALERT SENT for " + pair + " | RSI=" + rsi);
                        }

                        Thread.sleep(20);
                    } catch (Exception e) {
                        failed++;
                        System.err.println("Failed pair " + pair + ": " + e.getMessage());
                    }

                    if (processed % 20 == 0) {
                        System.out.println("Progress: " + processed + "/" + markets.size());
                    }
                }

                lastProcessed.set(processed);
                lastAlerts.set(alerts);
                lastFailed.set(failed);

                System.out.println("Scan complete.");
                System.out.println("Processed: " + processed);
                System.out.println("Alerts: " + alerts);
                System.out.println("Failed: " + failed);
                System.out.println("Next scan in " + freqMin + " minutes...");
            } catch (Exception e) {
                System.err.println("Scan job failed: " + e.getMessage());
                e.printStackTrace();
            }
        };

        scheduler.scheduleWithFixedDelay(job, 0, freqMin, TimeUnit.MINUTES);
        System.out.println("Scanner started. Running every " + freqMin + " minutes.");

        startWebServer(freqMin, interval, threshold, lastProcessed, lastAlerts, lastFailed);
    }

    private static void startWebServer(
            int freqMin,
            String interval,
            double threshold,
            AtomicInteger lastProcessed,
            AtomicInteger lastAlerts,
            AtomicInteger lastFailed) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/health", exchange -> writeJson(exchange, "{\"status\":\"ok\"}"));
        server.createContext("/", exchange -> {
            String response = "{"
                    + "\"service\":\"crypto-rsi-alert\","
                    + "\"status\":\"running\","
                    + "\"scan_frequency_minutes\":" + freqMin + ","
                    + "\"interval\":\"" + interval + "\","
                    + "\"threshold\":" + threshold + ","
                    + "\"last_processed\":" + lastProcessed.get() + ","
                    + "\"last_alerts\":" + lastAlerts.get() + ","
                    + "\"last_failed\":" + lastFailed.get()
                    + "}";
            writeJson(exchange, response);
        });

        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        System.out.println("Web status endpoint started on port " + port);
    }

    private static void writeJson(HttpExchange exchange, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String optional(Properties cfg, String key, String fallback) {
        String value = cfg.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }

        return fallback;
    }

    private static String require(Properties cfg, String key) {
        String value = cfg.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }

        throw new IllegalArgumentException(
                "Missing required config key: " + key + " (or env var " + envKey + ")");
    }
}
