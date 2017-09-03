package io.github.guggle.api;

import java.util.stream.DoubleStream;

public interface DoubleCacheView<K extends Permanent<K>> extends KeyView<K> {

    double value(K key);
    double get(K key);
    void put(K key, double val);
    DoubleStream values();

    default void put(K key, float val) {
        put(key, (double) val);
    }

    default float getFloat(K key) {
        return (float) get(key);
    }

}
