package io.github.guggle.utils;

public class Identity<T> {

    private final T val;
    private final int fnv;
    private final int oneAtATime;
    
    public Identity(final T val) {
        this.val = val;
        this.fnv = Fnv.start().hashObject(val).finish();
        this.oneAtATime = OneAtATime.start().hashObject(val).finish();
    }

    @Override
    public boolean equals(final Object rhs) {
        return val == rhs;
    }

    @Override
    public int hashCode() {
        return fnv;
    }

    public int fnvHash() {
        return fnv;
    }

    public int oneAtATimeHash() {
        return oneAtATime;
    }
}
