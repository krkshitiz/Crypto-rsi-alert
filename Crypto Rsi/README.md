# Crypto RSI Alert (Java)

This bot scans CoinDCX markets, computes RSI values, and sends email alerts when RSI crosses your configured threshold.

## Setup
1. Install Java 11+ and Maven.
2. Clone/copy this project.
3. Create your local config from the template:
   ```bash
   cp config.properties.example config.properties
   ```
4. Edit `config.properties` and fill in your real values:
   - `gmail.username`
   - `gmail.appPassword` (Gmail App Password)
   - `alert.recipient`
   - `rsi.period`
   - `rsi.threshold`
   - `scan.frequency.minutes`
   - `request.timeout.ms`
   - `limit.candles`
   - `interval`

## Run
```bash
mvn compile
mvn exec:java
```

The bot runs indefinitely. Press `Ctrl+C` to stop.

## Notes
- `config.properties` is ignored by git to prevent accidental secret commits.
- Candle data is pulled from CoinDCX public endpoints.
