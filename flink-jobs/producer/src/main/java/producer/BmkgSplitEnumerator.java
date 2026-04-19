package producer;

import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;

import java.io.IOException;
import java.util.Collections;

public class BmkgSplitEnumerator implements SplitEnumerator<BmkgSplit, Void> {

    private final SplitEnumeratorContext<BmkgSplit> context;

    public BmkgSplitEnumerator(SplitEnumeratorContext<BmkgSplit> context) {
        this.context = context;
    }

    @Override
    public void start() {
    }

    @Override
    public void handleSplitRequest(int subtaskId, String requesterHostname) {}

    @Override
    public void addSplitsBack(java.util.List<BmkgSplit> splits, int subtaskId) {}

    @Override
    public void addReader(int subtaskId) {
        context.assignSplit(new BmkgSplit("bmkg-split"), subtaskId);
    }

    @Override
    public Void snapshotState(long checkpointId) {
        return null;
    }

    @Override
    public void close() throws IOException {}
}
