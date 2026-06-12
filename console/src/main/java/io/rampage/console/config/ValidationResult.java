package io.rampage.console.config;

import java.util.List;

/**
 * The outcome of a config validation or save operation performed by
 * {@code ConfigEditor}.
 *
 * @param ok     {@code true} when validation and persistence both succeeded.
 * @param errors an immutable list of human-readable error messages; empty when {@code ok} is {@code true}.
 */
public record ValidationResult(boolean ok, List<String> errors) {

    /**
     * Returns a successful result with no errors.
     *
     * @return a valid result instance.
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    /**
     * Returns a failed result carrying the supplied error list.
     *
     * @param errors the list of validation error messages.
     * @return an invalid result instance.
     */
    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, List.copyOf(errors));
    }

    /**
     * Returns a failed result carrying a single error message.
     *
     * @param singleError the error message.
     * @return an invalid result instance.
     */
    public static ValidationResult invalid(String singleError) {
        return new ValidationResult(false, List.of(singleError));
    }
}
