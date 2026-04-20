# 🌏 Indonesia Earthquake Streaming Pipeline

## 🚀 Overview
Tested running in Ubuntu environment

Production-ready real-time data pipeline using: - Apache Flink 2.2.0 -
Redpanda (Kafka) - TimescaleDB - Nginx - Docker Compose

------------------------------------------------------------------------

## 🧱 Architecture

BMKG API → Producer → Kafka → Flink → TimescaleDB

------------------------------------------------------------------------

## ⚡ Quick Start

``` bash
make
```

Access in local deployment: 
- Flink: http://localhost:8081
- Redpanda: http://localhost:8080
- PgAdmin: http://localhost:5050

Access in cloud deployment
- Flink: http://localhost:8085/flink/
- Redpanda: http://localhost:8085/redpanda/
- PgAdmin: http://localhost:8085/pgadmin/

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