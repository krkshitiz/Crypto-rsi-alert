package com.cryptoalert;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Properties cfg = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            cfg.load(in);
        }

        String gmailUser = cfg.getProperty("gmail.username");
        String gmailAppPassword = cfg.getProperty("gmail.appPassword");
        String recipient = cfg.getProperty("alert.recipient", gmailUser);
        int rsiPeriod = Integer.parseInt(cfg.getProperty("rsi.period"));
        double threshold = Double.parseDouble(cfg.getProperty("rsi.threshold"));
        int freqMin = Integer.parseInt(cfg.getProperty("scan.frequency.minutes"));
        int timeout = Integer.parseInt(cfg.getProperty("request.timeout.ms"));
        int limit = Integer.parseInt(cfg.getProperty("limit.candles"));
        String interval = cfg.getProperty("interval"); // e.g., "5m"

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
                        // Fetch last 15 candles (1-minute intervals = last 15 minutes)
                        // RSI(14) calculated from most recent price action
                        List<Double> closes = candleClient.getClosePrices(pair, interval, limit);
                        if (closes.size() < rsiPeriod + 1) {
                            failed++;
                            continue;
                        }
                        // RSI calculated from the most recent closes (last ~15 minutes with 1m candles)
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
                e.printStackTrace();
            }
        };

        scheduler.scheduleWithFixedDelay(job, 0, freqMin, TimeUnit.MINUTES);
        System.out.println("Scanner started. Running every " + freqMin + " minutes.");
    }
}

