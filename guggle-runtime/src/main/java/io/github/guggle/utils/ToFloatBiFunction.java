package io.github.guggle.utils;

import java.util.function.ToDoubleBiFunction;

public interface ToFloatBiFunction<T,A> extends ToDoubleBiFunction<T,A> {

    default double applyAsDouble(final T target, final A args) {
        return (double) applyAsFloat(target, args);
    }
    
    float applyAsFloat(T target, A args);
}
