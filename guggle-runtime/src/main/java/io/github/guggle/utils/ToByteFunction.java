package io.github.guggle.utils;

import java.util.function.ToIntFunction;

public interface ToByteFunction<T> extends ToIntFunction<T> {

    default int applyAsInt(final T val) {
        return (int) applyAsByte(val);
    }

    byte applyAsByte(T val);
}
