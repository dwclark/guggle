package io.github.guggle.utils;

public class Identity {

    private final Object ref;
    private final int fnv;
    
    public Identity(final Object ref) {
        this.ref = ref;
        this.fnv = Fnv.start().hashObject(ref).finish();
    }

    @Override
    public boolean equals(final Object rhs) {
        return ref == rhs;
    }

    @Override
    public int hashCode() {
        return fnv;
    }
}
