package com.cryptoalert;

import java.util.List;

public class RsiCalculator {
    private RsiCalculator() {
        // Utility class
    }

    public static double computeRsi(List<Double> closes, int period) {
        if (closes == null) {
            throw new IllegalArgumentException("Close prices list cannot be null.");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("RSI period must be greater than 0.");
        }
        if (closes.size() < period + 1) {
            throw new IllegalArgumentException(
                    "Need at least period + 1 close values to compute RSI. Provided=" + closes.size());
        }

        // expects closes ordered oldest -> newest
        int len = closes.size();
        double gain = 0.0;
        double loss = 0.0;
        for (int i = 1; i <= period; i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            if (diff >= 0) {
                gain += diff;
            } else {
                loss += -diff;
            }
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        for (int i = period + 1; i < len; i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            double g = diff > 0 ? diff : 0;
            double l = diff < 0 ? -diff : 0;
            avgGain = (avgGain * (period - 1) + g) / period;
            avgLoss = (avgLoss * (period - 1) + l) / period;
        }

        if (avgLoss == 0) {
            return 100.0;
        }

        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
}
