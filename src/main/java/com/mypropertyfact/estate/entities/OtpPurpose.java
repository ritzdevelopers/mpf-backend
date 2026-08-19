package com.mypropertyfact.estate.entities;

/**
 * Discriminates OTP emails so registration / password-reset flows do not collide with legacy passwordless OTP.
 */
public enum OtpPurpose {
    /** Legacy passwordless sign-in / sign-up ({@code send-otp} / {@code verify-otp}). */
    MAGIC_LINK,
    /** Email + password registration: OTP verifies pending signup payload. */
    REGISTRATION,
    /** Forgot-password via OTP before choosing a new password. */
    PASSWORD_RESET,
    /** Broker portal login/register: OTP is sent to a phone (verified by backend). */
    PHONE_PORTAL_LOGIN
}
