package producer;

import org.apache.flink.api.connector.source.*;
import org.apache.flink.core.io.SimpleVersionedSerializer;

public class BmkgSource implements Source<String, BmkgSplit, Void> {

    @Override
    public Boundedness getBoundedness() {
        return Boundedness.CONTINUOUS_UNBOUNDED;
    }

    @Override
    public SourceReader<String, BmkgSplit> createReader(SourceReaderContext readerContext) {
        return new BmkgSourceReader(readerContext);
    }

    @Override
    public SplitEnumerator<BmkgSplit, Void> createEnumerator(
            SplitEnumeratorContext<BmkgSplit> enumContext) {
        return new BmkgSplitEnumerator(enumContext);
    }

    @Override
    public SplitEnumerator<BmkgSplit, Void> restoreEnumerator(
            SplitEnumeratorContext<BmkgSplit> enumContext,
            Void checkpoint) {
        return new BmkgSplitEnumerator(enumContext);
    }

    @Override
    public SimpleVersionedSerializer<BmkgSplit> getSplitSerializer() {
        return new BmkgSplitSerializer();
    }

    @Override
    public SimpleVersionedSerializer<Void> getEnumeratorCheckpointSerializer() {
        return new BmkgEnumeratorSerializer();
    }
}
