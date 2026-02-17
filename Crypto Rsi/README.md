# Crypto RSI Alert (Java)

This bot scans CoinDCX markets, computes RSI values, sends email alerts, and exposes web status endpoints for deployment.

## Setup
1. Install Java 11+ and Maven.
2. Clone/copy this project.
3. Create local config:
   ```bash
   cp config.properties.example config.properties
   ```
4. Fill real values in `config.properties`.

## Run locally
```bash
mvn compile
mvn exec:java
```

### Web endpoints
- `GET /health` → health check JSON.
- `GET /` → runtime status and last scan counters.

Default local URL: `http://localhost:8080`.

## Deploy on Render (Docker)
If Render does not show Java runtime for your account, deploy using Docker.

### Files already included
- `Dockerfile` (multi-stage build + runnable JAR)
- `.dockerignore`

### Render settings
1. New → **Web Service** → connect this repo.
2. Environment: **Docker**.
3. Leave build/start commands empty (Render uses Dockerfile).
4. Add environment variables listed below.
5. Deploy.

### Required environment variables
- `GMAIL_USERNAME`
- `GMAIL_APPPASSWORD`
- `ALERT_RECIPIENT`
- `RSI_PERIOD`
- `RSI_THRESHOLD`
- `SCAN_FREQUENCY_MINUTES`
- `REQUEST_TIMEOUT_MS`
- `LIMIT_CANDLES`
- `INTERVAL`

Render provides `PORT` automatically.

After deployment, open:
- `https://<your-app-domain>/health`
- `https://<your-app-domain>/`

## Notes
- `config.properties` is gitignored to avoid leaking secrets.
- Use a Gmail app password for SMTP.
