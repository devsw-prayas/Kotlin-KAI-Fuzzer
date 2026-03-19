package io.kai.seed;

import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;

public interface ISeedProvider {
    IBuilder next();
    boolean hasMore();
    void reset();
    NameRegistry getRegistry();
}
