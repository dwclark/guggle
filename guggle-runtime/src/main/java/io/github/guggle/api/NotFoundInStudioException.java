package io.github.guggle.api;

public class NotFoundInStudioException extends RuntimeException {

    public NotFoundInStudioException() {
        super();
    }

    public NotFoundInStudioException(final String str) {
        super(str);
    }
}
