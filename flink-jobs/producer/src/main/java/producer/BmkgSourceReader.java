package producer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.core.io.InputStatus;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BmkgSourceReader implements SourceReader<String, BmkgSplit> {

    private final SourceReaderContext context;
    private final OkHttpClient client;
    private final RateLimiter limiter;

    private volatile boolean running = true;

    private static final String URL = "https://data.bmkg.go.id/DataMKG/TEWS/gempadirasakan.json";

    public BmkgSourceReader(SourceReaderContext context) {
        this.context = context;

        this.client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .callTimeout(Duration.ofSeconds(10))
                .build();

        this.limiter = new RateLimiter(30);
    }

    @Override
    public void start() {}

    @Override
    public InputStatus pollNext(ReaderOutput<String> output) throws Exception {

        if (!running) {
            return InputStatus.END_OF_INPUT;
        }

        limiter.acquire();

        try {
            Request request = new Request.Builder().url(URL).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    output.collect(response.body().string());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return InputStatus.NOTHING_AVAILABLE; // 🔥 critical fix
    }

    @Override
    public List<BmkgSplit> snapshotState(long checkpointId) {
        return Collections.emptyList();
    }

    @Override
    public void addSplits(List<BmkgSplit> splits) {}

    @Override
    public void notifyNoMoreSplits() {}

    @Override
    public void close() {
        running = false;
    }

    @Override
    public CompletableFuture<Void> isAvailable() {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // polling interval
            } catch (InterruptedException ignored) {}
        });
    }
}
