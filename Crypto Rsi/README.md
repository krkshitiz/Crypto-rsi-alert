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

> If your Render service uses repository root as build context, this repo now includes a root-level `Dockerfile` so Render can build without changing Root Directory.

### Files already included
- `Dockerfile` (multi-stage build + runnable JAR)
- `.dockerignore`

### Render settings
1. New → **Web Service** → connect this repo.
2. Environment: **Docker**.
3. Leave build/start commands empty (Render uses Dockerfile).
4. Add environment variables listed below.
5. Deploy.

### If Render still shows `FileNotFoundException: config.properties`
- Confirm the service is deploying the latest commit from your selected branch.
- Trigger **Manual Deploy → Clear build cache & deploy** in Render.
- In Docker settings, keep **Root Directory** empty (repo root) so Render uses the root `Dockerfile`.

### Environment variables
For full scanner + email alerts, set:
- `GMAIL_USERNAME`
- `GMAIL_APPPASSWORD`
- `ALERT_RECIPIENT`

Optional tuning vars (defaults are used if omitted):
- `RSI_PERIOD` (default `14`)
- `RSI_THRESHOLD` (default `85`)
- `SCAN_FREQUENCY_MINUTES` (default `5`)
- `REQUEST_TIMEOUT_MS` (default `15000`)
- `LIMIT_CANDLES` (default `15`)
- `INTERVAL` (default `1m`)

Render provides `PORT` automatically. If Gmail vars are missing, the app still starts in `web_only` mode and `/` will show a message explaining what is missing.

After deployment, open:
- `https://<your-app-domain>/health`
- `https://<your-app-domain>/`

## Notes
- `config.properties` is gitignored to avoid leaking secrets.
- Use a Gmail app password for SMTP.
