package producer;

public class RateLimiter {

    private final long intervalMs;
    private long lastRequestTime = 0;

    public RateLimiter(int requestsPerMinute) {
        this.intervalMs = 60000 / requestsPerMinute;
    }

    public synchronized void acquire() throws InterruptedException {
        long now = System.currentTimeMillis();
        long wait = lastRequestTime + intervalMs -  now;

        if (wait > 0) {
            Thread.sleep(wait);
        }

        lastRequestTime = System.currentTimeMillis();
    }
}
