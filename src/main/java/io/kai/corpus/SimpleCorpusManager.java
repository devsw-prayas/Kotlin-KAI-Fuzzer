package io.kai.corpus;

import io.kai.contracts.IBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimpleCorpusManager implements ICorpusManager {
    private final HashMap<Long, IBuilder> index = new HashMap<>();
    private final List<IBuilder> corpus = new ArrayList<>();

    @Override
    public boolean add(IBuilder program, CorpusMeta meta) {
        if (index.containsKey(meta.structuralHash())) return false;
        index.put(meta.structuralHash(), program);
        corpus.add(program);
        return true;
    }

    @Override
    public List<IBuilder> all() {
        return corpus;
    }

    @Override
    public int size() {
        return corpus.size();
    }

    @Override
    public void evict() {
        // no-op for MVP
    }
}
