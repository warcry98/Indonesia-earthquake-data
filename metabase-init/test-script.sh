#!/bin/sh
set -e

MB_URL="http://localhost:3000"

# -----------------------------
# 🔐 Admin credentials (Metabase UI login)
# -----------------------------
ADMIN_EMAIL="admin@admin.com"
ADMIN_PASSWORD="admin123"

# -----------------------------
# 📦 TimescaleDB (data source)
# -----------------------------
DB_NAME="TimescaleDB"
DB_HOST="localhost"
DB_PORT="5432"
DB_DBNAME="postgres"
DB_USER="postgres"
DB_PASS="postgres"

echo "⏳ Waiting for Metabase..."
sleep 30

# ---------------------------------------
# 🚀 STEP 1: INITIAL SETUP
# ---------------------------------------

echo "🔍 Checking setup token..."

SETUP_TOKEN=$(curl -s "$MB_URL/api/session/properties" | jq -r '.setup-token')

if [ "$SETUP_TOKEN" != "null" ]; then
  echo "🆕 Running first-time setup..."

  curl -s -X POST "$MB_URL/api/setup" \
    -H "Content-Type: application/json" \
    -d "{
      \"token\": \"$SETUP_TOKEN\",
      \"user\": {
        \"email\": \"$ADMIN_EMAIL\",
        \"password\": \"$ADMIN_PASSWORD\",
        \"first_name\": \"Admin\",
        \"last_name\": \"User\"
      },
      \"database\": {
        \"engine\": \"postgres\",
        \"name\": \"$DB_NAME\",
        \"details\": {
          \"host\": \"$DB_HOST\",
          \"port\": $DB_PORT,
          \"dbname\": \"$DB_DBNAME\",
          \"user\": \"$DB_USER\",
          \"password\": \"$DB_PASS\"
        }
      }
    }"

  echo "✅ Metabase initialized!"
else
  echo "ℹ️ Already initialized"
fi

sleep 5

# ---------------------------------------
# 🔐 STEP 2: LOGIN
# ---------------------------------------

echo "🔐 Logging in..."

SESSION=$(curl -s -X POST "$MB_URL/api/session" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$ADMIN_EMAIL\",
    \"password\": \"$ADMIN_PASSWORD\"
  }" | jq -r '.id')

echo "Session: $SESSION"

# ---------------------------------------
# 📊 STEP 3: GET DATABASE ID
# ---------------------------------------

echo "📦 Fetching DB ID..."

DB_ID=$(curl -s "$MB_URL/api/database" \
  -H "X-Metabase-Session: $SESSION" | jq '.data[0].id')

echo "DB ID: $DB_ID"

# ---------------------------------------
# 📈 STEP 4: CREATE QUESTIONS
# ---------------------------------------

create_card () {
  NAME=$1
  QUERY=$2
  DISPLAY=$3

  curl -s -X POST "$MB_URL/api/card" \
    -H "X-Metabase-Session: $SESSION" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"$NAME\",
      \"dataset_query\": {
        \"type\": \"native\",
        \"native\": { \"query\": \"$QUERY\" },
        \"database\": $DB_ID
      },
      \"display\": \"$DISPLAY\"
    }" | jq -r '.id'
}

echo "📈 Creating charts..."

# 1️⃣ Magnitude over time (aggregated → much better visualization)
MAG_ID=$(create_card \
  "Magnitude Trend (Hourly Avg)" \
  "SELECT date_trunc('hour', time) AS hour,
          AVG(magnitude) AS avg_magnitude,
          COUNT(*) AS quake_count
   FROM earthquakes
   GROUP BY hour
   ORDER BY hour DESC" \
  "line")

# 2️⃣ Earthquake map (lat/lon + magnitude intensity)
MAP_ID=$(create_card \
  "Earthquake Locations" \
  "SELECT lat, lon, magnitude
   FROM earthquakes
   WHERE lat IS NOT NULL AND lon IS NOT NULL" \
  "map")

# 3️⃣ Magnitude distribution (bucketed histogram style)
DIST_ID=$(create_card \
  "Magnitude Distribution" \
  "SELECT floor(magnitude) AS mag_bucket,
          COUNT(*) AS count
   FROM earthquakes
   GROUP BY mag_bucket
   ORDER BY mag_bucket" \
  "bar")

# 4️⃣ Earthquakes by region (NEW 🔥)
REGION_ID=$(create_card \
  "Top Regions by Earthquakes" \
  "SELECT region,
          COUNT(*) AS total_quakes,
          AVG(magnitude) AS avg_magnitude
   FROM earthquakes
   GROUP BY region
   ORDER BY total_quakes DESC
   LIMIT 10" \
  "bar")

# 5️⃣ Depth distribution (NEW 🔥)
DEPTH_ID=$(create_card \
  "Depth Distribution" \
  "SELECT depth,
          COUNT(*) AS count
   FROM earthquakes
   GROUP BY depth
   ORDER BY count DESC" \
  "bar")

echo "Cards created:"
echo "$MAG_ID $MAP_ID $DIST_ID $REGION_ID $DEPTH_ID"

# ---------------------------------------
# 📊 STEP 5: CREATE DASHBOARD
# ---------------------------------------

DASH_ID=$(curl -s -X POST "$MB_URL/api/dashboard" \
  -H "X-Metabase-Session: $SESSION" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "🌏 Indonesia Earthquake Dashboard"
  }' | jq -r '.id')

echo "Dashboard ID: $DASH_ID"

# ---------------------------------------
# 📌 STEP 6: ADD CARDS
# ---------------------------------------

add_card () {
  CARD_ID=$1
  X=$2
  Y=$3

  curl -s -X POST "$MB_URL/api/dashboard/$DASH_ID/cards" \
    -H "X-Metabase-Session: $SESSION" \
    -H "Content-Type: application/json" \
    -d "{
      \"cardId\": $CARD_ID,
      \"sizeX\": 6,
      \"sizeY\": 4,
      \"row\": $Y,
      \"col\": $X
    }" > /dev/null
}

echo "📌 Adding cards..."

add_card $MAG_ID 0 0
add_card $MAP_ID 6 0
add_card $DIST_ID 0 4
add_card $REGION_ID 6 4
add_card $DEPTH_ID 0 8

echo "✅ Dashboard ready!"