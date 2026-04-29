package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.AdminPasswordResetEmailCheckRequest;
import com.mypropertyfact.estate.dtos.AdminPasswordResetSubmitRequest;
import com.mypropertyfact.estate.dtos.AdminPortalRegisterRequest;
import com.mypropertyfact.estate.dtos.LoginUserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dashboard / CMS staff authentication ({@code /api/v1/admin-portal/auth/**}).
 */
@RestController
@RequestMapping("/api/v1/admin-portal/auth")
@RequiredArgsConstructor
public class AdminPortalAuthApiController {

    private final AuthHubDelegate hub;

    @GetMapping("/admin-register-meta")
    public ResponseEntity<Map<String, Object>> adminRegisterMeta() {
        return hub.adminRegisterMeta();
    }

    @PostMapping("/admin-register")
    public ResponseEntity<?> adminRegister(@Valid @RequestBody AdminPortalRegisterRequest body) {
        return hub.adminRegister(body);
    }

    @PostMapping("/admin-password-reset-check-email")
    public ResponseEntity<?> checkAdminPasswordResetEmail(
            @Valid @RequestBody AdminPasswordResetEmailCheckRequest body) {
        return hub.checkAdminPasswordResetEmail(body);
    }

    @PostMapping("/admin-password-reset-request")
    public ResponseEntity<?> requestAdminPasswordReset(
            @Valid @RequestBody AdminPasswordResetSubmitRequest body) {
        return hub.requestAdminPasswordReset(body);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginUserDto loginUserDto,
            HttpServletResponse response) {
        return hub.authenticate(loginUserDto, response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        return hub.refreshToken(request);
    }

    @GetMapping("/session")
    public ResponseEntity<?> session(Authentication authentication, HttpServletRequest request) {
        return hub.session(authentication, request);
    }

    @GetMapping("/admin-permission-definitions")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> adminPermissionDefinitions() {
        return hub.adminPermissionDefinitions();
    }

    @GetMapping("/enquiry-access-status")
    public ResponseEntity<?> enquiryAccessStatus(
            Authentication authentication, HttpServletRequest request) {
        return hub.enquiryAccessStatus(authentication, request);
    }

    @PostMapping("/unlock-enquiries")
    public ResponseEntity<?> unlockEnquiries(
            @RequestBody Map<String, String> body,
            HttpServletResponse response,
            Authentication authentication) {
        return hub.unlockEnquiries(body, response, authentication);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        return hub.logout(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request,
            HttpServletResponse response) {
        return hub.refreshAccessToken(request, response);
    }
}
