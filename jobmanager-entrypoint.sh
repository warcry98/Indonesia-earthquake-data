#!/bin/bash
set -e

# Start JobManager in background
/docker-entrypoint.sh jobmanager &

# Wait until Flink REST API is ready
echo "Waiting for JobManager..."
until curl -s http://localhost:8081/overview > /dev/null; do
  sleep 2
done

echo "JobManager is up!"

# Submit producer job
flink run -d /opt/flink/producer.jar

# Optional delay
sleep 5

# Submit consumer job
flink run -d /opt/flink/consumer.jar

# Keep container alive (important!)
wait