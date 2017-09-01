package io.github.guggle.api;

import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;
import java.util.stream.DoubleStream;

public interface DoubleCacheView<K extends Permanent<K>> extends KeyView<K> {
    double value(K key, ToDoubleFunction<K> generate);
    double get(K key);
    void put(K key, double val);
    DoubleStream values();
}
