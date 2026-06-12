package io.rampage.factory;

import java.util.List;

/**
 * Thrown when a secret or credential reference cannot be resolved during
 * configuration loading.
 *
 * <p>Carries a list of individual error messages so that callers can report
 * all resolution failures at once rather than failing on the first.
 */
public class SecretResolutionException extends RuntimeException {
    /** The individual resolution error messages. */
    private final List<String> errors;

    /**
     * Constructs the exception with a summary message and a list of individual
     * resolution error details.
     *
     * @param message a summary description of the failure
     * @param errors  the individual resolution errors; copied defensively
     */
    public SecretResolutionException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    /**
     * Constructs the exception with a single error message, which is also used
     * as the sole entry in the errors list.
     *
     * @param message the error message
     */
    public SecretResolutionException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    /**
     * Returns the individual resolution error messages.
     *
     * @return an unmodifiable list of error messages; never {@code null}
     */
    public List<String> getErrors() {
        return errors;
    }
}
