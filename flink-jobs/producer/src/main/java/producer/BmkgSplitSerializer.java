package producer;

import org.apache.flink.core.io.SimpleVersionedSerializer;

import java.io.IOException;

public class BmkgSplitSerializer implements SimpleVersionedSerializer<BmkgSplit> {

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public byte[] serialize(BmkgSplit split) throws IOException {
        return split.splitId().getBytes();
    }

    @Override
    public BmkgSplit deserialize(int version, byte[] serialized) throws IOException {
        return new BmkgSplit(new String(serialized));
    }
}
