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
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) throws Exception {
        Properties cfg = loadConfig();

        AtomicInteger lastProcessed = new AtomicInteger();
        AtomicInteger lastAlerts = new AtomicInteger();
        AtomicInteger lastFailed = new AtomicInteger();
        AtomicReference<String> mode = new AtomicReference<>("starting");

        int freqMin = parseInt(optional(cfg, "scan.frequency.minutes", "5"), 5);
        double threshold = parseDouble(optional(cfg, "rsi.threshold", "85"), 85);
        String interval = optional(cfg, "interval", "1m");

        String scannerSetupError = startScannerIfConfigured(
                cfg,
                freqMin,
                threshold,
                interval,
                lastProcessed,
                lastAlerts,
                lastFailed,
                mode
        );

        startWebServer(freqMin, interval, threshold, lastProcessed, lastAlerts, lastFailed, mode, scannerSetupError);
    }

    private static Properties loadConfig() {
        Properties cfg = new Properties();

        try (InputStream in = new FileInputStream("config.properties")) {
            cfg.load(in);
            System.out.println("Loaded configuration from config.properties");
        } catch (Exception e) {
            System.out.println("config.properties not found. Using environment variables / defaults.");
        }

        return cfg;
    }

    private static String startScannerIfConfigured(
            Properties cfg,
            int freqMin,
            double threshold,
            String interval,
            AtomicInteger lastProcessed,
            AtomicInteger lastAlerts,
            AtomicInteger lastFailed,
            AtomicReference<String> mode) {

        String gmailUser = optional(cfg, "gmail.username", null);
        String gmailAppPassword = optional(cfg, "gmail.appPassword", null);
        String recipient = optional(cfg, "alert.recipient", gmailUser);

        if (isBlank(gmailUser) || isBlank(gmailAppPassword) || isBlank(recipient)) {
            mode.set("web_only");
            String message = "Scanner disabled: set GMAIL_USERNAME, GMAIL_APPPASSWORD and ALERT_RECIPIENT to enable alerts.";
            System.out.println(message);
            return message;
        }

        int rsiPeriod = parseInt(optional(cfg, "rsi.period", "14"), 14);
        int timeout = parseInt(optional(cfg, "request.timeout.ms", "15000"), 15000);
        int limit = parseInt(optional(cfg, "limit.candles", "15"), 15);

        CoinDCXClient dcx = new CoinDCXClient(timeout);
        CoinDCXCandleClient candleClient = new CoinDCXCandleClient(timeout);
        EmailNotifier emailNotifier = new EmailNotifier(gmailUser, gmailAppPassword, recipient);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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
        mode.set("scanner_running");
        System.out.println("Scanner started. Running every " + freqMin + " minutes.");
        return null;
    }

    private static void startWebServer(
            int freqMin,
            String interval,
            double threshold,
            AtomicInteger lastProcessed,
            AtomicInteger lastAlerts,
            AtomicInteger lastFailed,
            AtomicReference<String> mode,
            String scannerSetupError) throws Exception {
        int port = parseInt(System.getenv().getOrDefault("PORT", "8080"), 8080);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/health", exchange -> writeJson(exchange, "{\"status\":\"ok\"}"));
        server.createContext("/", exchange -> {
            String response = "{"
                    + "\"service\":\"crypto-rsi-alert\"," 
                    + "\"status\":\"running\"," 
                    + "\"mode\":\"" + mode.get() + "\"," 
                    + "\"scan_frequency_minutes\":" + freqMin + ","
                    + "\"interval\":\"" + interval + "\"," 
                    + "\"threshold\":" + threshold + ","
                    + "\"last_processed\":" + lastProcessed.get() + ","
                    + "\"last_alerts\":" + lastAlerts.get() + ","
                    + "\"last_failed\":" + lastFailed.get();

            if (scannerSetupError != null) {
                response += ",\"message\":\"" + scannerSetupError.replace("\"", "'") + "\"";
            }

            response += "}";
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
        if (!isBlank(value)) {
            return value.trim();
        }

        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (!isBlank(envValue)) {
            return envValue.trim();
        }

        return fallback;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
