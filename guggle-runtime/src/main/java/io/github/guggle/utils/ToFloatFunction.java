package io.github.guggle.utils;

import java.util.function.ToDoubleFunction;

public interface ToFloatFunction<T> extends ToDoubleFunction<T> {

default double applyAsDouble(final T val) {
        return (double) applyAsFloat(val);
    }
    
    float applyAsFloat(T val);
}
