package io.github.guggle.api;

public interface Expiration {
    boolean expired(int now);
    void accessed(int now);
}
