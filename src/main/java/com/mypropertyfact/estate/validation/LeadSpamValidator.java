package com.mypropertyfact.estate.validation;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rejects clearly fake / test enquiry payloads so they never hit Telegram / CRM.
 * Mirrors frontend rules in my-property-fact {@code src/lib/leadValidation.js}.
 */
public final class LeadSpamValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s'-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Set<String> BLOCKED_NAMES = Set.of(
            "test", "test user", "testuser", "testing", "tester",
            "dummy", "dummy user", "sample", "sample user",
            "demo", "demo user", "fake", "fake user",
            "asdf", "asdf asdf", "qwerty", "abc", "abc abc",
            "xxxx", "xxxxx", "user", "user name", "your name", "full name",
            "name", "n a", "na", "none", "null", "undefined",
            "john doe", "jane doe", "john smith", "foo bar"
    );

    private static final Set<String> BLOCKED_NAME_TOKENS = Set.of(
            "test", "testing", "tester", "dummy", "sample", "demo", "fake",
            "asdf", "qwerty", "xxx", "xxxx"
    );

    private static final Set<String> BLOCKED_EMAIL_LOCALS = Set.of(
            "test", "testing", "tester", "testuser", "dummy", "sample", "demo",
            "fake", "asdf", "qwerty", "abc", "noreply", "no-reply", "donotreply",
            "user", "username", "email", "mail", "admin", "xyz", "xxx"
    );

    private static final Set<String> BLOCKED_EMAIL_DOMAINS = Set.of(
            "example.com", "example.org", "example.net", "test.com", "test.in",
            "mailinator.com", "guerrillamail.com", "tempmail.com", "temp-mail.org",
            "10minutemail.com", "yopmail.com", "trashmail.com", "sharklasers.com"
    );

    private static final Set<String> BLOCKED_PHONES = Set.of(
            "9876543210", "9876543211", "9123456789", "9988776655",
            "9000000000", "9999999999", "8888888888", "7777777777", "6666666666",
            "1234567890", "0123456789", "1111111111", "2222222222", "3333333333",
            "4444444444", "5555555555", "0000000000", "9898989898", "9090909090",
            "9812345678"
    );

    private LeadSpamValidator() {}

    /**
     * @return rejection message, or {@code null} if the lead looks acceptable
     */
    public static String rejectReason(String name, String email, String phone) {
        String nameError = validateName(name);
        if (nameError != null) {
            return nameError;
        }
        String emailError = validateEmail(email);
        if (emailError != null) {
            return emailError;
        }
        return validatePhone(phone);
    }

    public static String validateName(String name) {
        if (!StringUtils.hasText(name)) {
            return "Name is required";
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2) {
            return "Name must be at least 2 characters";
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            return "Name can only contain letters, spaces, hyphens, and apostrophes";
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (BLOCKED_NAMES.contains(normalized)) {
            return "Please enter a valid name";
        }
        for (String token : normalized.split(" ")) {
            if (BLOCKED_NAME_TOKENS.contains(token)) {
                return "Please enter a valid name";
            }
        }
        String compact = trimmed.replaceAll("\\s+", "");
        if (compact.length() > 1 && compact.chars().allMatch(c -> Character.toLowerCase(c) == Character.toLowerCase(compact.charAt(0)))) {
            return "Please enter a valid name";
        }
        return null;
    }

    public static String validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "Email is required";
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return "Please enter a valid email address";
        }
        String[] parts = trimmed.split("@", 2);
        String localRaw = parts[0];
        String domain = parts.length > 1 ? parts[1] : "";
        String local = localRaw.contains("+") ? localRaw.substring(0, localRaw.indexOf('+')) : localRaw;
        String localBase = local.replaceAll("[._-]", "");

        if (BLOCKED_EMAIL_LOCALS.contains(local)
                || BLOCKED_EMAIL_LOCALS.contains(localBase)
                || local.matches("test(\\d+)?")
                || localBase.matches("test(\\d+)?")
                || local.contains("dummy")
                || local.contains("sample")
                || local.contains("fakeuser")
                || "fake".equals(local)) {
            return "Please enter a valid email address";
        }
        if (BLOCKED_EMAIL_DOMAINS.contains(domain)) {
            return "Please enter a valid email address";
        }
        return null;
    }

    public static String validatePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "Phone number is required";
        }
        String cleaned = normalizeIndianPhone(phone);
        if (!cleaned.matches("\\d+")) {
            return "Please enter a valid phone number";
        }
        if (cleaned.length() != 10) {
            return "Phone number must be exactly 10 digits (after country code)";
        }
        char first = cleaned.charAt(0);
        if (first < '6' || first > '9') {
            return "Phone number must start with 6, 7, 8, or 9";
        }
        if (cleaned.chars().allMatch(c -> c == cleaned.charAt(0))) {
            return "Please enter a valid phone number";
        }
        if (isSequentialDigits(cleaned) || BLOCKED_PHONES.contains(cleaned)) {
            return "Please enter a valid phone number";
        }
        return null;
    }

    static String normalizeIndianPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() >= 12) {
            return digits.substring(digits.length() - 10);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        return digits;
    }

    private static boolean isSequentialDigits(String digits) {
        if (digits.length() != 10) {
            return false;
        }
        boolean asc = true;
        boolean desc = true;
        for (int i = 1; i < digits.length(); i++) {
            int prev = digits.charAt(i - 1) - '0';
            int curr = digits.charAt(i) - '0';
            if ((prev + 1) % 10 != curr) {
                asc = false;
            }
            if ((prev + 9) % 10 != curr) {
                desc = false;
            }
        }
        return asc || desc;
    }
}
