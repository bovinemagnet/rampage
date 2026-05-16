package io.rampage.console.config;

import java.util.List;

public record ValidationResult(boolean ok, List<String> errors) {

    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, List.copyOf(errors));
    }

    public static ValidationResult invalid(String singleError) {
        return new ValidationResult(false, List.of(singleError));
    }
}
