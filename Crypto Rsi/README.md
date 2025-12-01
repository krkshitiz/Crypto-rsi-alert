# Crypto RSI Alert (Java)

This program fetches 4-hour candles for coins from CryptoCompare, computes RSI(14) on 4h candles, and sends an email if RSI > 85.

## Setup
1. Install Java 11+ and Maven.
2. Clone/copy this project.
3. Edit `config.properties` and fill:
   - gmail.username = contactkshitiznow@gmail.com
   - gmail.appPassword = <your Gmail App Password (create in Google account)>
   - alert.recipient = <where to send alerts; can be same as gmail.username>
   - rsi.threshold = 85
   - binance.interval = 4h
   - scan.frequency.minutes = 60
   - cryptocompare.apikey = <YOUR_CRYPTOCOMPARE_API_KEY>

4. Run:
   mvn compile
   mvn exec:java

The bot runs indefinitely. To stop press Ctrl+C.

Notes:
- This project uses CryptoCompare for 4-hour OHLC candles. Place your CryptoCompare API key into `config.properties` as `cryptocompare.apikey` (do NOT paste the key into chat).
- If you want WhatsApp alerts later, we can add Twilio sandbox integration.

