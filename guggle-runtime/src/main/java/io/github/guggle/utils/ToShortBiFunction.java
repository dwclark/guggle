package io.github.guggle.utils;

import java.util.function.ToIntBiFunction;

public interface ToShortBiFunction<T,A> extends ToIntBiFunction<T,A> {

    default int applyAsInt(final T target, final A args) {
        return (int) applyAsShort(target, args);
    }

    short applyAsShort(T target, A args);
}
