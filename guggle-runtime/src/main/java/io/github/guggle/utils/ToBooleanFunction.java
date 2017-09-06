package io.github.guggle.utils;

import java.util.function.ToIntFunction;

public interface ToBooleanFunction<T> extends ToIntFunction<T> {
    default int applyAsInt(T val) {
        return applyAsBoolean(val) == true ? 1 : 0;
    }

    default boolean from(final int val) {
        return val == 1 ? true : false;
    }
    
    boolean applyAsBoolean(T val);
}
