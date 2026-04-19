package consumer;

import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PostgresSink implements Sink<BmkgEvent> {

    @Override
    public SinkWriter<BmkgEvent> createWriter(WriterInitContext context) {
        return new PostgresSinkWriter();
    }

    static class PostgresSinkWriter implements SinkWriter<BmkgEvent> {

        private static final int BATCH_SIZE = 100;
        private static final long FLUSH_INTERVAL_MS = 2000;

        private Connection conn;
        private PreparedStatement ps;

        private int batchCount = 0;
        private long lastFlushTime = System.currentTimeMillis();

        public PostgresSinkWriter() {
            try {
                conn = DriverManager.getConnection(
                        "jdbc:postgresql://timescaledb:5432/postgres",
                        "postgres",
                        "postgres"
                );

                conn.setAutoCommit(false); // 🔥 important for batching

                ps = conn.prepareStatement(
                        "INSERT INTO earthquakes(time, magnitude, depth, lat, lon, region) " +
                                "VALUES (?, ?, ?, ?, ?, ?) " +
                                "ON CONFLICT (time, magnitude, lat, lon) DO NOTHING"
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(BmkgEvent e, Context context) {
            if (e == null || e.time == null) {
                return; // skip bad data safely
            }

            try {
                ps.setTimestamp(1, e.time);
                ps.setDouble(2, e.magnitude);
                ps.setString(3, e.depth);
                ps.setDouble(4, e.lat);
                ps.setDouble(5, e.lon);
                ps.setString(6, e.region);

                ps.addBatch();
                batchCount++;

                long now = System.currentTimeMillis();

                // 🚀 Flush conditions
                if (batchCount >= BATCH_SIZE || (now - lastFlushTime) >= FLUSH_INTERVAL_MS) {
                    flushBatch();
                }

            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void flushBatch() throws SQLException {
            if (batchCount == 0) return;

            ps.executeBatch();
            conn.commit();

            batchCount = 0;
            lastFlushTime = System.currentTimeMillis();
        }

        @Override
        public void flush(boolean endOfInput) {
            try {
                flushBatch(); // ✅ ensures even single record gets inserted
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() throws Exception {
            flushBatch(); // ✅ flush remaining

            ps.close();
            conn.close();
        }
    }
}