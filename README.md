# 🌏 Indonesia Earthquake Streaming Pipeline

## 🚀 Overview

Production-ready real-time data pipeline using: - Apache Flink 2.2.0 -
Redpanda (Kafka) - TimescaleDB - Metabase (auto dashboards) - Nginx
(TLS + OAuth) - Docker Compose

------------------------------------------------------------------------

## 🧱 Architecture

BMKG API → Producer → Kafka → Flink → TimescaleDB → Metabase (via Nginx)

------------------------------------------------------------------------

## ⚡ Quick Start

``` bash
make
```

Access: 
- https://localhost:8443/flink/ 
- https://localhost:8443/metabase/

------------------------------------------------------------------------

## 🔐 Security

-   Google OAuth (oauth2-proxy)
-   Self-signed TLS (HTTPS)
-   Rate limiting
-   Security headers

------------------------------------------------------------------------

## 📊 Dashboards

Auto-created: - 🌍 Earthquake Map - 📈 Magnitude Over Time - 📊
Magnitude Distribution

------------------------------------------------------------------------

## 🐳 Services

-   Flink JobManager & TaskManager
-   Redpanda
-   TimescaleDB
-   Metabase
-   Nginx
-   OAuth2 Proxy

------------------------------------------------------------------------

## 🛠 Commands

``` bash
make build
make up
make down
make logs
```

------------------------------------------------------------------------

## 📦 Database Schema

``` sql
CREATE TABLE earthquakes (
    time TIMESTAMPTZ,
    magnitude DOUBLE PRECISION,
    depth TEXT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    region TEXT,
    PRIMARY KEY (time, magnitude, lat, lon)
);
```

------------------------------------------------------------------------

## 🔧 Notes

-   Accept browser warning for self-signed cert
-   Ensure OAuth redirect URI matches config