package com.cryptoalert;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        Properties cfg = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            cfg.load(in);
        }

        String gmailUser = require(cfg, "gmail.username");
        String gmailAppPassword = require(cfg, "gmail.appPassword");
        String recipient = cfg.getProperty("alert.recipient", gmailUser);
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
    }

    private static String require(Properties cfg, String key) {
        String value = cfg.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }
        return value.trim();
    }
}
