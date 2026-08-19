package com.mypropertyfact.estate.validation;

/**
 * Normalizes Indian mobile numbers to 10 digits (no country code).
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    public static String normalize(String phone) {
        if (phone == null) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() >= 12) {
            digits = digits.substring(digits.length() - 10);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        if (digits.length() != 10) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits");
        }
        if (!digits.matches("^[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Please enter a valid Indian mobile number");
        }
        return digits;
    }
}
