package io.github.guggle.utils;

import java.util.function.ToIntBiFunction;

public interface ToByteBiFunction<T,A> extends ToIntBiFunction<T,A> {

    default int applyAsInt(final T target, final A args) {
        return (int) applyAsByte(target, args);
    }

    byte applyAsByte(T target, A args);
}
