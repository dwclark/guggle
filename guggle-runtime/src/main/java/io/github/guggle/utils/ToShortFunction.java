package io.github.guggle.utils;

import java.util.function.ToIntFunction;

public interface ToShortFunction<T> extends ToIntFunction<T> {
    default int applyAsInt(T val) {
        return (int) applyAsShort(val);
    }

    short applyAsShort(T val);
}
