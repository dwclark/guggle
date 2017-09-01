package io.github.guggle.api;

import java.util.function.ToLongFunction;
import java.util.stream.Stream;
import java.util.stream.LongStream;

public interface LongCacheView<K extends Permanent<K>> extends KeyView<K> {

    public interface Holder {
        long getLong();
    }
    
    Holder value(K key, ToLongFunction<K> generate);
    Holder get(K key);
    void put(K key, long val);
    LongStream values();
}
