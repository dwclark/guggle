package io.github.guggle.api;

public class OverloadedException extends RuntimeException {

    public OverloadedException() {
        super();
    }

    public OverloadedException(final String message) {
        super(message);
    }
}
