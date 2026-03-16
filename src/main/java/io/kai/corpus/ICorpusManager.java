package io.kai.corpus;

import io.kai.contracts.IBuilder;

import java.util.List;

public interface ICorpusManager {
    boolean add(IBuilder program, CorpusMeta meta);
    List<IBuilder> all();
    int size();
    void evict();
}
