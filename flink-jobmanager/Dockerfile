FROM maven:3.9.14-eclipse-temurin-21-noble AS build
COPY ../flink-jobs /app/src
RUN mvn -f /app/src/producer/pom.xml clean package
RUN mvn -f /app/src/consumer/pom.xml clean package

FROM flink:2.2.0-scala_2.12-java21

USER root

RUN apt-get update && apt-get install -y wget tar && \
    wget https://github.com/async-profiler/async-profiler/releases/download/v4.3/async-profiler-4.3-linux-x64.tar.gz && \
    tar -xzf async-profiler-4.3-linux-x64.tar.gz && \
    mv async-profiler-4.3-linux-x64 /opt/async-profiler && \
    rm async-profiler-4.3-linux-x64.tar.gz

# Permissions
RUN chmod -R 755 /opt/async-profiler

# create writable log directory
RUN mkdir -p /opt/flink/log && \
    chown -R flink:flink /opt/flink/log

COPY --chown=flink:flink ../flink-config.yaml /opt/flink/conf/config.yaml

USER flink

ADD https://jdbc.postgresql.org/download/postgresql-42.7.3.jar /opt/flink/lib/

COPY --from=build /app/src/producer/target/producer-1.0-SNAPSHOT.jar /opt/flink/producer.jar
COPY --from=build /app/src/consumer/target/consumer-1.0-SNAPSHOT.jar /opt/flink/consumer.jar

# Copy entrypoint
COPY ../jobmanager-entrypoint.sh /opt/flink/jobmanager-entrypoint.sh
RUN chmod +x /opt/flink/jobmanager-entrypoint.sh

ENTRYPOINT ["/opt/flink/jobmanager-entrypoint.sh"]