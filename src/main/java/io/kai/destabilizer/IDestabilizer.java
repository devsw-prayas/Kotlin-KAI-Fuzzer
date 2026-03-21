package io.kai.destabilizer;

import io.kai.contracts.IBuilder;

import java.util.List;
import java.util.Random;

public interface IDestabilizer {
    String id();
    boolean canApply(IBuilder root);
    void destabilize(IBuilder root, Random rng);
    default List<String> requiredFlags() { return List.of(); }
}
