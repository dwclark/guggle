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

    default short getShort(final K key) {
        return (short) get(key);
    }

    default void put(final K key, final byte val) {
        put(key, (int) val);
    }

    default byte getByte(final K key) {
        return (byte) get(key);
    }

    default void put(final K key, final boolean val) {
        put(key, val ? 1 : 0);
    }

    default boolean getBoolean(final K key) {
        return get(key) != 0 ? true : false;
    }
}
