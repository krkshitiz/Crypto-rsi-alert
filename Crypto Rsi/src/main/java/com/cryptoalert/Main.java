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
        int rsiPeriod = Integer.parseInt(cfg.getProperty("rsi.period", "14"));
        double threshold = Double.parseDouble(cfg.getProperty("rsi.threshold", "85.0"));
        //String interval = cfg.getProperty("binance.interval", "4h");
        int freqMin = Integer.parseInt(cfg.getProperty("scan.frequency.minutes", "60"));
        String apiKey = cfg.getProperty("cryptocompare.apikey");

        CryptoCompareClient cryptoCompare = new CryptoCompareClient(Integer.parseInt(cfg.getProperty("request.timeout.ms", "15000")), apiKey);
        EmailNotifier emailNotifier = new EmailNotifier(gmailUser, gmailAppPassword, recipient);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        int delayBetweenScans = Integer.parseInt(cfg.getProperty("delay.between.scans.minutes", "5"));

        Runnable job = () -> {
            try {
                long startTime = System.currentTimeMillis();
                System.out.println("[" + LocalDateTime.now() + "] Starting scan of ALL symbols...");
                List<String> symbols = cryptoCompare.getAllSymbols();
                System.out.println("Found " + symbols.size() + " symbols to scan");
                int alerted = 0;
                int processed = 0;
                int failed = 0;
                
                for (String sym : symbols) {
                    try {
                        processed++;
                        if (processed % 50 == 0) {
                            System.out.println("Progress: " + processed + "/" + symbols.size() + " symbols processed...");
                        }
                        List<Double> closes = cryptoCompare.get5MinClosePrices(sym, "USDT", 300);
                        if (closes.size() < rsiPeriod + 1) {
                            failed++;
                            continue;
                        }
                        double rsi = RsiCalculator.computeRsi(closes, rsiPeriod);
                        if (rsi > threshold) {
                            alerted++;
                            String subj = "ALERT: " + sym + "/USDT RSI=" + String.format("%.2f", rsi);
                            String body = sym + "/USDT has RSI " + String.format("%.2f", rsi) + " (threshold " + threshold + ")\nTime: " + LocalDateTime.now();
                            emailNotifier.sendEmail(subj, body);
                            System.out.println("ALERT SENT for " + sym + "/USDT RSI=" + String.format("%.2f", rsi));
                        }
                        // polite small pause to avoid too many instant requests
                        Thread.sleep(20);
                    } catch (Exception e) {
                        failed++;
                        // continue with other symbols
                    }
                }
                long duration = (System.currentTimeMillis() - startTime) / 1000;
                long durationMinutes = duration / 60;
                System.out.println("[" + LocalDateTime.now() + "] Scan complete!");
                System.out.println("  - Processed: " + processed + " symbols");
                System.out.println("  - Failed/Skipped: " + failed);
                System.out.println("  - Alerts sent: " + alerted);
                System.out.println("  - Duration: " + durationMinutes + "m " + (duration % 60) + "s");
                System.out.println("Next scan will start in " + delayBetweenScans + " minutes...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Use scheduleWithFixedDelay so it waits for completion before scheduling next
        // This ensures each full scan completes before starting the next one
        scheduler.scheduleWithFixedDelay(job, 0, delayBetweenScans, TimeUnit.MINUTES);
        System.out.println("Scheduler started. Will scan ALL symbols, then wait " + delayBetweenScans + " minutes before next scan.");
    }
}

