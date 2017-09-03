package io.github.guggle.api;

import java.util.stream.IntStream;

public interface IntCacheView<K extends Permanent<K>> extends KeyView<K> {

    public interface Holder {
        int intValue();
        
        default short shortValue() {
            return (short) intValue();
        }
        
        default byte byteValue() {
            return (byte) intValue();
        }

        default boolean booleanValue() {
            return intValue() == 0 ? false : true;
        }
    }
    
    Holder value(K key);
    Holder get(K key);
    void put(K key, int val);
    IntStream values();
}
