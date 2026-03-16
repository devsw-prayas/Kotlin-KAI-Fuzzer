package io.kai.seed;

import io.kai.contracts.IBuilder;

public interface ISeedProvider {
    IBuilder next();
    boolean hasMore();
    void reset();
}
