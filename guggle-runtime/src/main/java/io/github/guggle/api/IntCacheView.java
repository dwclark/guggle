package io.github.guggle.api;

import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import java.util.stream.IntStream;

public interface IntCacheView<K extends Permanent<K>> extends KeyView<K> {

    public interface Holder {
        int getInt();
        
        default short getShort() {
            return (short) getInt();
        }
        
        default byte getByte() {
            return (byte) getInt();
        }
    }
    
    Holder value(K key, ToIntFunction<K> generate);
    Holder get(K key);
    void put(K key, int val);
    IntStream values();
}
