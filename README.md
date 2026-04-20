# 🌏 Indonesia Earthquake Streaming Pipeline

## 🚀 Overview
Created to gather Indonesia Earthquake data from BMKG for Earthquake pattern analysis and Seismic analysis.

Tested running in Ubuntu environment

Production-ready real-time data pipeline using: - Apache Flink 2.2.0 -
Redpanda (Kafka) - TimescaleDB - Nginx - Docker Compose

------------------------------------------------------------------------

## 🧱 Architecture

BMKG API → Producer (Flink Java) → Kafka → Consumer (Flink Java) → TimescaleDB

------------------------------------------------------------------------

## ⚡ Quick Start

``` bash
make
```

Access in local deployment: 
- Flink: http://localhost:8081
- Redpanda: http://localhost:8080
- PgAdmin: http://localhost:5050
Follow authentication in .env

Access in cloud deployment
- Flink: http://18.233.190.224:8085/flink/
- Redpanda: http://18.233.190.224:8085/redpanda/
- PgAdmin: http://18.233.190.224:8085/pgadmin/
Use username = admin and password = admin123 to access

------------------------------------------------------------------------

## 🐳 Services

-   Flink JobManager & TaskManager
-   Redpanda
-   TimescaleDB
-   Nginx

------------------------------------------------------------------------

## 🛠 Commands

``` bash
make env
make build
make up
```

OR

```bash
make all
```
------------------------------------------------------------------------

## 📦 Database Schema

``` sql
CREATE TABLE IF NOT E→ Metabase (via Nginx)XISTS earthquakes (
    time timestamp with time zone,
    magnitude DOUBLE PRECISION,
    depth TEXT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    region TEXT,
    PRIMARY KEY (time, magnitude, lat, lon)
);

SELECT create_hyperta→ Metabase (via Nginx)ble('earthquakes', 'time', if_not_exists => TRUE);

DO $$
    BEGIN
        PERFORM add_retention_policy('earthquakes', interval '6 months');
    EXCEPTION
        WHEN others THEN
            NULL;
    END $$;
```

------------------------------------------------------------------------