package io.github.guggle.api;

import java.util.stream.LongStream;

public interface LongCacheView<K extends Permanent<K>> extends KeyView<K> {
    
    long value(K key);
    long get(K key);
    void put(K key, long val);
    LongStream values();
}
