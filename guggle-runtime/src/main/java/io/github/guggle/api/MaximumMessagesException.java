package io.github.guggle.api;

public class MaximumMessagesException extends RuntimeException {

    public MaximumMessagesException() {
        super();
    }

    public MaximumMessagesException(final String message) {
        super(message);
    }
}
