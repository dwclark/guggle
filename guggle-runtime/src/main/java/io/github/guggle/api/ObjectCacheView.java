package io.github.guggle.api;

import java.util.function.Function;
import java.util.stream.Stream;

public interface ObjectCacheView<K extends Permanent<K>, V> extends KeyView<K> {
    V value(K key, Function<K,V> generate);
    V get(K key);
    void put(K key, V val);
    Stream<V> values();
}
