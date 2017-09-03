package io.github.guggle.api;

import java.util.stream.Stream;

public interface ObjectCacheView<K extends Permanent<K>, V> extends KeyView<K> {

    public interface Holder<V> {
        V get(Class<V> type);
    }
    
    Holder<V> value(K key);
    Holder<V> get(K key);
    void put(K key, V val);
    Stream<V> values();
}
