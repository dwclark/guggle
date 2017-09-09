package io.github.guggle.api;

import java.util.stream.IntStream;

public interface IntCacheView<K extends Permanent<K>> extends KeyView<K> {

    int value(K key);
    int get(K key);
    void put(K key, int val);
    IntStream values();

    default void put(final K key, final short val) {
        put(key, (int) val);
    }

    default short shortValue(final K key) {
        return (short) value(key);
    }

    default void put(final K key, final byte val) {
        put(key, (int) val);
    }

    default byte byteValue(final K key) {
        return (byte) value(key);
    }

    default void put(final K key, final boolean val) {
        put(key, val ? 1 : 0);
    }

    default boolean booleanValue(final K key) {
        return value(key) == 1 ? true : false;
    }
}
