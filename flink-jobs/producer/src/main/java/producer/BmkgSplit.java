package producer;

import org.apache.flink.api.connector.source.SourceSplit;

public class BmkgSplit implements SourceSplit {

    private final String id;

    public BmkgSplit(String id) {
        this.id = id;
    }

    @Override
    public String splitId() {
        return id;
    }
}
