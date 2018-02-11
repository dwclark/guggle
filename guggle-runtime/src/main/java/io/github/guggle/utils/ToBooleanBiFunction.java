package io.github.guggle.utils;

import java.util.function.ToIntBiFunction;

public interface ToBooleanBiFunction<T,A> extends ToIntBiFunction<T,A> {

    default int applyAsInt(T target, A args) {
        return applyAsBoolean(target, args) == true ? 1 : 0;
    }

    default boolean from(final int val) {
        return val == 1 ? true : false;
    }
    
    boolean applyAsBoolean(T target, A args);
}
