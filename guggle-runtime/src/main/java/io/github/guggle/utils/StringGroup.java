package io.github.guggle.utils;

public class StringGroup {

    private final String ref;
    private final int hash;
    
    public StringGroup(final String ref) {
        this.ref = ref;
        this.hash = ref.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if(!(o instanceof StringGroup)) {
            return false;
        }

        StringGroup rhs = (StringGroup) o;
        return ref.equals(rhs.ref);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
