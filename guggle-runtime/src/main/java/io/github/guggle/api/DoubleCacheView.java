package io.github.guggle.api;

import java.util.stream.DoubleStream;

public interface DoubleCacheView<K extends Permanent<K>> extends KeyView<K> {

    public interface Holder {
        double doubleValue();

        default float floatValue() {
            return (float) doubleValue();
        }
    }
    
    double value(K key);
    double get(K key);
    void put(K key, double val);
    DoubleStream values();
}
