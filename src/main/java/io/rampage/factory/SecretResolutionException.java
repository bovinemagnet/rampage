package io.rampage.factory;

import java.util.List;

public class SecretResolutionException extends RuntimeException {
    private final List<String> errors;

    public SecretResolutionException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public SecretResolutionException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public List<String> getErrors() {
        return errors;
    }
}
