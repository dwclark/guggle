package io.github.guggle.utils;

import java.util.AbstractQueue;

public abstract class ExemplarQueue<T> extends AbstractQueue<T> {
    public abstract T exemplar();
}
