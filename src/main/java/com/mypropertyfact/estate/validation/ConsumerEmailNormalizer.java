package com.mypropertyfact.estate.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

/**
 * Normalizes consumer emails for OTP and app registration using the same rules as Jakarta Bean
 * Validation {@link Email} (widely used in Spring apps and aligned with typical HTML5-style checks).
 */
public final class ConsumerEmailNormalizer {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private ConsumerEmailNormalizer() {}

    /**
     * Trim, validate format, then lowercase ({@link Locale#ROOT}) for consistent OTP rows and DB lookups.
     *
     * @throws IllegalArgumentException if missing or not a valid email address
     */
    public static String normalize(String rawEmail) {
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        String trimmed = rawEmail.trim();
        if (!VALIDATOR.validateValue(EmailProbe.class, "value", trimmed).isEmpty()) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static final class EmailProbe {
        @Email(message = "Please enter a valid email address")
        @NotBlank
        @SuppressWarnings("unused")
        private String value;
    }
}
