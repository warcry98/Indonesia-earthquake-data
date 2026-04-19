package producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class DeduplicationFunction extends KeyedProcessFunction<String, String, String> {

    private transient ValueState<String> lastId;
    private transient ObjectMapper mapper;

    @Override
    public void processElement(String value, Context ctx, Collector<String> out) throws Exception {

        System.out.println("RAW: " + value);

        if (value == null || value.isEmpty()) {
            return; // skip bad input
        }

        if (mapper == null) {
            mapper = new ObjectMapper();
        }

        if (lastId == null) {
            lastId = getRuntimeContext()
                    .getState(new ValueStateDescriptor<>("lastId", String.class));
        }

        JsonNode node;
        try {
            node = mapper.readTree(value);
        } catch (Exception e) {
            System.out.println("JSON PARSE ERROR!");
            e.printStackTrace(); // 🔥 show the real issue
            return;
        }

        System.out.println("NODE: " + node);

        String dateTime = node.path("DateTime").asText(null);
        String coordinates = node.path("Coordinates").asText(null);
        String magnitude = node.path("Magnitude").asText(null);

        // skip if critical fields missing
        if (dateTime == null || coordinates == null || magnitude == null) {
            return;
        }

        String id = dateTime + coordinates + magnitude;

        String last = lastId.value();

        System.out.println("ID: " + id);

        if (last == null || !last.equals(id)) {
            lastId.update(id);
            out.collect(value);
        }
    }
}