package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.ForgotPasswordRequest;
import com.mypropertyfact.estate.dtos.RegisterUserDto;
import com.mypropertyfact.estate.dtos.ResetPasswordRequest;
import com.mypropertyfact.estate.dtos.TokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Consumer / mobile / public site authentication ({@code /api/v1/app/auth/**}).
 */
@RestController
@RequestMapping("/api/v1/app/auth")
@RequiredArgsConstructor
public class AppAuthApiController {

    private final AuthHubDelegate hub;

    @PostMapping("/signup")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterUserDto dto, HttpServletResponse response) {
        return hub.register(dto, response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        return hub.forgotPassword(body);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        return hub.resetPassword(body);
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody TokenRequest tokenRequest,
            HttpServletResponse response) throws Exception {
        return hub.googleLogin(tokenRequest, response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        return hub.verifyToken(authHeader);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        return hub.refreshToken(request);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@RequestBody Map<String, String> request) {
        return hub.sendOTP(request);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTPAndRegister(@RequestBody Map<String, String> request) {
        return hub.verifyOTPAndRegister(request);
    }

    /** Optional: same JWT session shape as dashboard for shared cookies. */
    @GetMapping("/session")
    public ResponseEntity<?> session(Authentication authentication, HttpServletRequest request) {
        return hub.session(authentication, request);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        return hub.logout(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        return hub.refreshAccessToken(request, response);
    }
}
