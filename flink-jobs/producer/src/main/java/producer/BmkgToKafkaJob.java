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

package producer;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
public class BmkgToKafkaJob {
	final static String kafkaTopic = "bmkg-raw";

	public static void main(String[] args) throws Exception {

		final String bootstrapServers = args.length > 0 ? args[0] : "redpanda:29092";

		// Ensure topic exists
		createTopicIfNotExists(bootstrapServers, kafkaTopic, 1, (short) 1);

		// Sets up the execution environment, which is the main entry point
		// to building Flink applications.
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		env.enableCheckpointing(1000);

		DataStream<String> stream = env.fromSource(
				new BmkgSource(),
				WatermarkStrategy.noWatermarks(),
				"bmkg-source"
		).setParallelism(1);

//		DataStream<String> deduplicated = stream;

		DataStream<String> deduplicated = stream
				.keyBy(value -> "bmkg")
				.process(new DeduplicationFunction());

		KafkaSink<String> sink = KafkaSink.<String>builder()
				.setBootstrapServers(bootstrapServers)
				.setRecordSerializer(
						KafkaRecordSerializationSchema.builder()
								.setTopic(kafkaTopic)
								.setValueSerializationSchema(new SimpleStringSchema())
								.build()
				)
				.build();

		deduplicated.sinkTo(sink);

		env.execute("BMKG -> Kafka");
	}

	// Topic creation logic
	private static void createTopicIfNotExists(
			String bootstrapServers,
			String topicName,
			int partitions,
			short replicationFactor
	) throws ExecutionException, InterruptedException {

		Properties props = new Properties();
		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

		try (AdminClient adminClient = AdminClient.create(props)) {

			Set<String> existingTopics = adminClient.listTopics().names().get();

			if (!existingTopics.contains(topicName)) {
				System.out.println("Creating topic: " + topicName);

				NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);

				adminClient.createTopics(Collections.singleton(newTopic)).all().get();

				System.out.println("Topic created: " + topicName);
			} else {
				System.out.println("Topic already exists: " + topicName);
			}
		}
	}
}
