/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.jdbc.core.datastream.sink.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;

/**
 * Skeleton for a Flink DataStream Job.
 *
 * <p>For a tutorial how to write a Flink application, check the
 * tutorials and examples on the <a href="https://flink.apache.org">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class KafkaToTimescaleJob {
	final static String kafkaTopic = "bmkg-raw";

	public static void main(String[] args) throws Exception {
		final String bootstrapServers = System.getenv().getOrDefault("RP_BOOTSTRAP_SERVERS", "redpanda:29092");


		initDatabase();
		// Sets up the execution environment, which is the main entry point
		// to building Flink applications.
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		env.enableCheckpointing(1000);

		Properties props = kafkaAuthProps();

		KafkaSource<String> source = KafkaSource.<String>builder()
				.setBootstrapServers(bootstrapServers)
				.setTopics(kafkaTopic)
				.setGroupId("bmkg-group-v2")
				.setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.setProperties(props)
				.build();

		DataStream<String> stream = env.fromSource(
				source,
				WatermarkStrategy.noWatermarks(),
				"Kafka Source"
		);

		DataStream<BmkgEvent> parsed = stream.map(new RichMapFunction<String, BmkgEvent>() {

			private transient ObjectMapper mapper;
			private  transient DateTimeFormatter formatter;
			private transient ZoneId zone;

			@Override
			public BmkgEvent map(String json) throws Exception {

				if (json == null || json.isEmpty()) {
					return null; // skip bad message
				}

				if (mapper == null) {
					mapper = new ObjectMapper();
				}

				if (formatter == null) {
					formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
				}

				if (zone == null) {
					zone = ZoneId.of("Asia/Jakarta");
				}

				JsonNode node;
				try {
					node = mapper.readTree(json);
				} catch (Exception e) {
					// skip invalid JSON
					return null;
				}

				String datetime = node.path("DateTime").asText(null);

				// ❌ Missing or invalid DateTime
				if (datetime == null || datetime.isEmpty()) {
					return null;
				}

				BmkgEvent e = new BmkgEvent();

				try {
					Instant instant = Instant.parse(datetime);
					e.time = Timestamp.from(instant);
				} catch (Exception ex) {
					return null; // skip bad time format
				}

				e.magnitude = node.path("Magnitude").asDouble(0.0);
				e.depth = node.path("Kedalaman").asText("");

				String coordStr = node.path("Coordinates").asText("0,0");
				String[] coord = coordStr.split(",");

				try {
					e.lat = coord.length > 0 ? Double.parseDouble(coord[0]) : 0.0;
					e.lon = coord.length > 1 ? Double.parseDouble(coord[1]) : 0.0;
				} catch (Exception ex) {
					e.lat = 0.0;
					e.lon = 0.0;
				}

				e.region = node.path("Wilayah").asText("");

				return e;
			}
		});

		parsed
				.filter(Objects::nonNull)
				.sinkTo(
						new PostgresSink()
				);

		env.execute("Kafka -> TimescaleDB");
	}

	private static void initDatabase() throws ClassNotFoundException {
		// Load driver explicitly
		Class.forName("org.postgresql.Driver");

		String url = System.getenv("JDBC_URL");
		String user = System.getenv("TIMESCALEDB_USER");
		String pass = System.getenv("TIMESCALEDB_PASSWORD");

		try (Connection conn = DriverManager.getConnection(
				url,
				user,
				pass
		);
			 Statement stmt = conn.createStatement()) {

			stmt.execute("""
            CREATE TABLE IF NOT EXISTS earthquakes (
                time timestamp with time zone,
                magnitude DOUBLE PRECISION,
                depth TEXT,
                lat DOUBLE PRECISION,
                lon DOUBLE PRECISION,
                region TEXT,
                PRIMARY KEY (time, magnitude, lat, lon)
            );
        """);

			stmt.execute("""
            SELECT create_hypertable('earthquakes', 'time', if_not_exists => TRUE);
        """);

			stmt.execute("""
            DO $$
                BEGIN
                	PERFORM add_retention_policy('earthquakes', interval '6 months');
                EXCEPTION
                    WHEN others THEN
                        NULL;
                END $$;
        """);

			System.out.println("✅ TimescaleDB initialized");
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

	private static Properties kafkaAuthProps() {
		Properties props = new Properties();

		String username = System.getenv("RP_USER");
		String password = System.getenv("RP_PASSWORD");

		props.put("security.protocol", "SASL_PLAINTEXT");
		props.put("sasl.mechanism", "SCRAM-SHA-256");

		props.put(
				"sasl.jaas.config",
				"org.apache.kafka.common.security.scram.ScramLoginModule required " +
						"username=\"" + username + "\" password=\"" + password + "\";"
		);

		return props;
	}
}
