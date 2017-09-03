package io.github.guggle.api;

import java.util.stream.LongStream;

public interface LongCacheView<K extends Permanent<K>> extends KeyView<K> {

    public interface Holder {
        long longValue();
    }
    
    Holder value(K key);
    Holder get(K key);
    void put(K key, long val);
    LongStream values();
}
