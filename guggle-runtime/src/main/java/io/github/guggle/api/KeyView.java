package io.github.guggle.api;

import java.util.stream.Stream;

public interface KeyView<K extends Permanent<K>> {
    boolean contains(K key);
    void remove(K key);
    Stream<K> keys();
}
