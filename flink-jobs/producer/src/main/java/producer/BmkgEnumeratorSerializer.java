package producer;

import org.apache.flink.core.io.SimpleVersionedSerializer;

public class BmkgEnumeratorSerializer implements SimpleVersionedSerializer<Void> {

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public byte[] serialize(Void obj) {
        return new byte[0];
    }

    @Override
    public Void deserialize(int version, byte[] serialized) {
        return null;
    }
}
