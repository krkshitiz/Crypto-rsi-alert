package com.cryptoalert;

import java.util.List;

public class RsiCalculator {
    public static double computeRsi(List<Double> closes, int period) {
        // expects closes ordered oldest -> newest
        int len = closes.size();
        double gain = 0.0, loss = 0.0;
        for (int i = 1; i <= period; i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            if (diff >= 0) gain += diff; else loss += -diff;
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
        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
}

