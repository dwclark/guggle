package io.github.guggle.api;

import java.util.stream.Stream;

public interface KeyView<K extends Permanent<K>> {
    boolean contains(K key);
    boolean remove(K key);
    void dirty(K key);
    Stream<K> keys();
    int size();
    Lifetime getLifetime();
    void setLifetime(Lifetime val);
}
