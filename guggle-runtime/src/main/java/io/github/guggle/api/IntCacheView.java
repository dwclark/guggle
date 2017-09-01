package io.github.guggle.api;

import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import java.util.stream.IntStream;

public interface IntCacheView<K extends Permanent<K>> extends KeyView<K> {
    int value(K key, ToIntFunction<K> generate);
    int get(K key);
    void put(K key, int val);
    IntStream values();
}
