package com.mypropertyfact.estate.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mypropertyfact.estate.dtos.AdminPasswordResetEmailCheckRequest;
import com.mypropertyfact.estate.dtos.AdminPasswordResetSubmitRequest;
import com.mypropertyfact.estate.dtos.AdminPortalRegisterRequest;
import com.mypropertyfact.estate.dtos.ForgotPasswordRequest;
import com.mypropertyfact.estate.dtos.LoginResponse;
import com.mypropertyfact.estate.dtos.LoginUserDto;
import com.mypropertyfact.estate.dtos.RegisterUserDto;
import com.mypropertyfact.estate.dtos.ResetPasswordRequest;
import com.mypropertyfact.estate.dtos.TokenRequest;
import com.mypropertyfact.estate.entities.MasterRole;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.MasterRoleRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import com.mypropertyfact.estate.services.AdminPermissionService;
import com.mypropertyfact.estate.services.AdminPasswordResetRequestService;
import com.mypropertyfact.estate.services.AuthenticationService;
import com.mypropertyfact.estate.services.EnquiryAccessService;
import com.mypropertyfact.estate.security.AdminPermissionKeys;
import com.mypropertyfact.estate.services.JwtService;
import com.mypropertyfact.estate.services.OTPService;
import com.mypropertyfact.estate.services.SendEmailHandler;
import com.mypropertyfact.estate.services.UserRoleService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

@Slf4j
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    @Value("${google.client.id}")
    private String googleClientId;
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private final UserRoleService userRoleService;
    private final UserRepository userRepository;
    private final MasterRoleRepository masterRoleRepository;
    private final OTPService otpService;
    private final PasswordEncoder passwordEncoder;
    private final SendEmailHandler sendEmailHandler;
    private final AdminPermissionService adminPermissionService;
    private final EnquiryAccessService enquiryAccessService;
    private final AdminPasswordResetRequestService adminPasswordResetRequestService;

    @Value("${cookies.domain:}")
    private String cookiesDomain;

    @Value("${http.secure}")
    private boolean httpSecure;

    @Value("${security.jwt.expiration-time}")
    private long accessTokenExpiration;
    @Value("${security.jwt.refresh.expiration-time}")
    private long refreshTokenExpiration;

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody RegisterUserDto registerUserDto,
            HttpServletResponse response) {
        final User registeredUser;
        try {
            registeredUser = authenticationService.signup(registerUserDto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }

        String jwtToken = jwtService.generateToken(registeredUser);
        String refreshToken = jwtService.generateRefreshToken(registeredUser);
        response.addHeader("Set-Cookie",
                buildAuthCookie("token", jwtToken, accessTokenExpiration / 1000).toString());
        response.addHeader("Set-Cookie",
                buildAuthCookie("refreshToken", refreshToken, refreshTokenExpiration / 1000).toString());

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwtToken);
        loginResponse.setRefreshToken(refreshToken);
        loginResponse.setExpiresIn(jwtService.getExpirationTime());
        loginResponse.setUser(registeredUser);
        return ResponseEntity.ok(loginResponse);
    }

    /** Consumer/mobile: request password reset email (always 200 {@code ok: true} to avoid email enumeration). */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        authenticationService.sendConsumerPasswordResetEmail(body.getEmail());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** Consumer/mobile: submit token from reset link and set a new password. */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        try {
            authenticationService.resetPasswordWithConsumerToken(body.getToken(), body.getNewPassword());
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /** Role list + whether PIN is required (see {@code app.admin.registration-pin}). */
    @GetMapping("/admin-register-meta")
    public ResponseEntity<Map<String, Object>> adminRegisterMeta() {
        return ResponseEntity.ok(authenticationService.getAdminRegisterMeta());
    }

    /**
     * Self-registration from /admin/register. Defaults to USER only when {@code roleIds} is omitted.
     * Optional ADMIN requires dashboard username. SUPERADMIN is not allowed.
     */
    @PostMapping("/admin-register")
    public ResponseEntity<?> adminRegister(@Valid @RequestBody AdminPortalRegisterRequest body) {
        try {
            User user = authenticationService.registerAdminPortalUser(body);
            boolean hasPendingAdmin = user.getRoles() != null && user.getRoles().stream()
                    .anyMatch(r -> r != null && r.getRoleName() != null
                            && "ADMIN".equalsIgnoreCase(r.getRoleName())
                            && Boolean.TRUE.equals(r.getIsActive()))
                    && Boolean.FALSE.equals(user.getAdminStaffApproved());
            String message = hasPendingAdmin
                    ? "Account created. Your Admin access request is pending approval by a Super Admin. "
                    + "You cannot sign in to the admin dashboard until it is approved."
                    : "Account created successfully";
            return ResponseEntity.ok(Map.of(
                    "message", message,
                    "email", user.getEmail(),
                    "pendingAdminApproval", hasPendingAdmin));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * Forgot-password step 1: verify the email exists and is eligible for admin password reset.
     */
    @PostMapping("/admin-password-reset-check-email")
    public ResponseEntity<?> checkAdminPasswordResetEmail(
            @Valid @RequestBody AdminPasswordResetEmailCheckRequest body) {
        try {
            adminPasswordResetRequestService.checkEmailEligibleForPasswordReset(body.getEmail());
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * Admin dashboard forgot-password: stores a bcrypt hash for super-admin review (no plaintext persisted).
     */
    @PostMapping("/admin-password-reset-request")
    public ResponseEntity<?> requestAdminPasswordReset(@Valid @RequestBody AdminPasswordResetSubmitRequest body) {
        try {
            adminPasswordResetRequestService.submitRequest(body);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Request recorded. A Super Administrator will review your new password."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginUserDto loginUserDto,
            HttpServletResponse response) {
        final User authenticatedUser;
        try {
            authenticatedUser = authenticationService.authenticate(loginUserDto);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        }
        String jwtToken = jwtService.generateToken(authenticatedUser);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);
        response.addHeader("Set-Cookie", buildAuthCookie("token", jwtToken, accessTokenExpiration / 1000).toString());
        response.addHeader("Set-Cookie",
                buildAuthCookie("refreshToken", refreshToken, refreshTokenExpiration / 1000).toString());
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwtToken);
        loginResponse.setRefreshToken(refreshToken);
        loginResponse.setExpiresIn(jwtService.getExpirationTime());
        loginResponse.setUser(authenticatedUser);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody TokenRequest tokenRequest,
        HttpServletResponse response ) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()).setAudience(Collections.singletonList(googleClientId)).build();

        GoogleIdToken idToken = verifier.verify(tokenRequest.getToken());

        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            User user;
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                // User already registered
                user = existingUser.get();
            } else {
                // Register new user
                RegisterUserDto registerUserDto = new RegisterUserDto();
                registerUserDto.setEmail(email);
                // Ensure fullName is not null - use email prefix or default if name is not
                // available
                String userFullName = (name != null && !name.trim().isEmpty())
                        ? name.trim()
                        : (email != null ? email.split("@")[0] : "User");
                registerUserDto.setFullName(userFullName);
                registerUserDto.setPassword(UUID.randomUUID().toString()); // random password since using Google login
                user = authenticationService.signupWithoutPassword(registerUserDto);
            }
            String jwtToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            response.addHeader("Set-Cookie", buildAuthCookie("token", jwtToken, accessTokenExpiration / 1000).toString());
            response.addHeader("Set-Cookie",
                    buildAuthCookie("refreshToken", refreshToken, refreshTokenExpiration / 1000).toString());
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(jwtToken);
            loginResponse.setRefreshToken(refreshToken);
            loginResponse.setExpiresIn(jwtService.getExpirationTime());
            loginResponse.setUser(user);
            return ResponseEntity.ok(loginResponse);
        } else {
            throw new RuntimeException("Invalid Google token");
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7); // remove "Bearer "

            // Verify token
            Claims claims = jwtService.validateToken(token); // custom util (explained below)

            // Extract roles from token
            Set<String> roles = jwtService.extractRoles(token);
            List<String> rolesList = new ArrayList<>(roles);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("email", claims.getSubject());
            response.put("expiresAt", claims.getExpiration().toString());
            response.put("roles", rolesList);

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Refresh token is required"));
        }

        try {
            Claims claims = jwtService.validateToken(refreshToken);
            String username = claims.getSubject();

            Optional<User> userDetails = userRepository.findByEmail(username);

            if (!userDetails.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }

            User user = userDetails.get();

            // Generate new access + refresh tokens
            String jwtToken = jwtService.generateToken(user);
            String refToken = jwtService.generateRefreshToken(user);

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(jwtToken);
            loginResponse.setRefreshToken(refToken);
            loginResponse.setExpiresIn(jwtService.getExpirationTime());
            loginResponse.setUser(user);

            return ResponseEntity.ok(loginResponse);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token has expired"));
        } catch (io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.security.SignatureException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        } catch (Exception e) {
            log.error("Error refreshing token: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Failed to refresh token"));
        }
    }

    /**
     * Send OTP to mobile number
     * POST /auth/send-otp
     * Body: { "phoneNumber": "+911234567890" }
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is required", "message", "Please enter your email"));
            }

            // Generate and send OTP (validation happens inside OTPService)
            String otpCode = otpService.generateOTP(email);
            String body = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <style>
                        body {
                        font-family: Arial, sans-serif;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 0;
                        }
                        .container {
                        max-width: 600px;
                        background: #ffffff;
                        padding: 20px;
                        border-radius: 8px;
                        box-shadow: 0 0 10px rgba(0,0,0,0.1);
                        }
                        .header {
                        text-align: center;
                        font-size: 22px;
                        font-weight: bold;
                        color: #2c3e50;
                        margin-bottom: 15px;
                        }
                        .otp-box {
                        text-align: center;
                        font-size: 26px;
                        font-weight: bold;
                        letter-spacing: 3px;
                        background: #eef2ff;
                        padding: 10px;
                        border-radius: 6px;
                        margin: 20px 0;
                        color: #1f2937;
                        }
                        .message {
                        font-size: 14px;
                        color: #555;
                        line-height: 1.6;
                        }
                        .footer {
                        margin-top: 20px;
                        font-size: 13px;
                        color: #777;
                        }
                    </style>
                    </head>
                    <body>
                    <div class="container">
                        <div class="header">Welcome to My Property Fact</div>

                        <p class="message">
                        Hello,<br><br>
                        Your One-Time Password (OTP) to securely log in is:
                        </p>

                        <div class="otp-box">%s</div>

                        <p class="message">
                        Please do not share this OTP with anyone. It is valid for a limited time only.
                        If you did not request this, please ignore this email.
                        </p>

                        <div class="footer">
                        Regards,<br>
                        <strong>My Property Fact Team</strong>
                        </div>
                    </div>
                    </body>
                    </html>
                    """.formatted(otpCode);
            sendEmailHandler.sendEmail(email, "OTP for MyPropertyFact", body);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP sent successfully",
                    // "otp", otpCode,
                    "expiresIn", 300 // 5 minutes
            ));

        } catch (IllegalArgumentException e) {
            // User-friendly validation errors
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(), "message", e.getMessage()));
        } catch (Exception e) {
            // Check for database/data truncation errors
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Data truncation")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid phone number format",
                                "message", "Please enter a valid 10-digit phone number"));
            }
            // Generic error message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send OTP",
                            "message", "Unable to send OTP. Please check your phone number and try again."));
        }
    }

    /**
     * Verify OTP and register/login user
     * POST /auth/verify-otp
     * Body: { "phoneNumber": "+911234567890", "otp": "123456", "fullName": "John
     * Doe" }
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTPAndRegister(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otpCode = request.get("otp");
            String fullName = request.get("fullName");

            if (email == null || email.isEmpty() ||
                    otpCode == null || otpCode.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email and OTP are required",
                                "message", "Please enter both email and OTP"));
            }

            // Verify OTP (phone number will be normalized inside verifyOTP)
            boolean isValid = otpService.verifyOTP(email, otpCode);

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid or expired OTP",
                                "message",
                                "The OTP you entered is incorrect or has expired. Please request a new OTP."));
            }

            // Check if user exists
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;
            String userStatus;

            if (existingUser.isPresent()) {
                // User exists - login
                user = existingUser.get();
                userStatus = "existing";
            } else {
                // New user - register
                user = new User();
                // Ensure id is null so it can be auto-generated
                user.setId(null);
                user.setEmail(email);
                // Ensure fullName is not null or empty
                String userFullName = (fullName != null && !fullName.trim().isEmpty())
                        ? fullName.trim()
                        : "User";
                user.setFullName(userFullName);
                // Generate a random email if not provided
                user.setEmail(email);
                // Generate a secure random password
                String randomPassword = UUID.randomUUID().toString();
                user.setPassword(passwordEncoder.encode(randomPassword));

                // Set default USER role
                Set<MasterRole> roles = new HashSet<>();
                masterRoleRepository.findByRoleNameIgnoreCase("USER")
                        .ifPresentOrElse(
                                roles::add,
                                () -> {
                                    // Create USER role if it doesn't exist
                                    MasterRole userRole = new MasterRole();
                                    userRole.setRoleName("USER");
                                    userRole.setDescription("Default user role");
                                    userRole.setIsActive(true);
                                    roles.add(masterRoleRepository.save(userRole));
                                });
                user.setRoles(roles);
                user.setVerified(true); // Verified via OTP

                user = userRepository.save(user);
                userStatus = "new";
            }

            // Generate JWT token
            String jwtToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(jwtToken);
            loginResponse.setRefreshToken(refreshToken);
            loginResponse.setExpiresIn(jwtService.getExpirationTime());

            // Return response
            Map<String, Object> response = new HashMap<>();
            response.put("status", userStatus);
            response.put("token", jwtToken);
            response.put("refreshToken", refreshToken);
            response.put("expiresIn", jwtService.getExpirationTime());
            // Get role names from MasterRole entities
            List<String> roleNames = user.getRoles() != null
                    ? user.getRoles().stream()
                            .filter(role -> role != null && role.getIsActive() != null && role.getIsActive())
                            .map(role -> "ROLE_" + role.getRoleName())
                            .toList()
                    : List.of("ROLE_USER");

            // Use HashMap instead of Map.of() to handle null values (phone can be null for
            // email-based registration)
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("fullName", user.getFullName() != null ? user.getFullName() : "");
            userMap.put("phone", user.getPhone() != null ? user.getPhone() : "");
            userMap.put("email", user.getEmail() != null ? user.getEmail() : "");
            userMap.put("role", roleNames.isEmpty() ? "ROLE_USER" : roleNames.get(0));
            userMap.put("roles", roleNames);
            userMap.put("verified", user.getVerified() != null ? user.getVerified() : false);
            response.put("user", userMap);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Get the root cause of the exception
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }

            String errorMessage = rootCause.getMessage();
            String userFriendlyMessage;

            // Check for specific database errors and provide user-friendly messages
            if (errorMessage != null) {
                if (errorMessage.contains("Field 'id' doesn't have a default value") ||
                        errorMessage.contains("doesn't have a default value")) {
                    userFriendlyMessage = "Unable to create your account. Please contact support or try again later.";
                } else if (errorMessage.contains("Data truncation")) {
                    userFriendlyMessage = "Invalid data provided. Please check your information and try again.";
                } else if (errorMessage.contains("Duplicate entry") || errorMessage.contains("already exists")) {
                    userFriendlyMessage = "An account with this phone number already exists. Please sign in instead.";
                } else if (errorMessage.contains("ConstraintViolationException") ||
                        errorMessage.contains("constraint")) {
                    userFriendlyMessage = "Invalid information provided. Please check your details and try again.";
                } else {
                    // Generic user-friendly message for other errors
                    userFriendlyMessage = "Unable to complete your registration. Please try again or contact support if the problem persists.";
                }
            } else {
                userFriendlyMessage = "Unable to complete your registration. Please try again or contact support if the problem persists.";
            }

            // Log the actual error for debugging (but don't expose it to users)
            // Note: This is a database configuration issue - the users table id column
            // needs AUTO_INCREMENT
            System.err.println("Error verifying OTP: " + errorMessage);
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed",
                            "message", userFriendlyMessage));
        }
    }

    public String getMethodName(@RequestParam String param) {
        return new String();
    }

    @GetMapping("/session")
    public ResponseEntity<?> session(Authentication authentication, HttpServletRequest request) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String expiresAt = jwtService.getExpiryFromCookie(request);
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("email", authentication.getName());
        body.put("roles", roles);
        body.put("expiresAt", expiresAt);

        userRepository.findByEmail(authentication.getName())
                .ifPresent(user -> {
                    body.put("userId", user.getId());
                    body.put("fullName", user.getFullName());
                    body.put("dashboardUsername", user.getDashboardUsername());
                    body.put("permissions",
                            user.getAdminPermissions() != null
                                    ? new ArrayList<>(user.getAdminPermissions())
                                    : new ArrayList<String>());
                });

        return ResponseEntity.ok(body);
    }

    /**
     * Labels and keys for Super Admin when editing Admin users (Manage Users).
     */
    @GetMapping("/admin-permission-definitions")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> adminPermissionDefinitions() {
        return ResponseEntity.ok(AdminPermissionKeys.definitions());
    }

    /**
     * Whether the current admin may use enquiries APIs and if they must enter the 4-digit PIN first.
     */
    @GetMapping("/enquiry-access-status")
    public ResponseEntity<?> enquiryAccessStatus(
            Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (userRoleService.userHasRole(user.getId(), "SUPERADMIN")) {
            return ResponseEntity.ok(Map.of(
                    "hasPermission", true,
                    "needsCode", false,
                    "unlocked", true));
        }
        boolean hasPerm = adminPermissionService.can(user, AdminPermissionKeys.MANAGE_ENQUIRIES);
        if (!hasPerm) {
            return ResponseEntity.ok(Map.of(
                    "hasPermission", false,
                    "needsCode", false,
                    "unlocked", false));
        }
        User db = userRepository.findById(user.getId()).orElse(user);
        boolean pinConfigured = db.getEnquiryAccessPinHash() != null && !db.getEnquiryAccessPinHash().isBlank();
        boolean unlocked = enquiryAccessService.canAccessEnquiries(db, request);
        return ResponseEntity.ok(Map.of(
                "hasPermission", true,
                "needsCode", pinConfigured,
                "pinConfigured", pinConfigured,
                "unlocked", unlocked));
    }

    /**
     * Validates the 4-digit PIN and sets the HttpOnly {@code enquiryUnlock} cookie (ADMIN with permission only).
     */
    @PostMapping("/unlock-enquiries")
    public ResponseEntity<?> unlockEnquiries(
            @RequestBody Map<String, String> body,
            HttpServletResponse response,
            Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (userRoleService.userHasRole(user.getId(), "SUPERADMIN")) {
            return ResponseEntity.ok(Map.of("unlocked", true));
        }
        if (!adminPermissionService.can(user, AdminPermissionKeys.MANAGE_ENQUIRIES)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You do not have enquiries access."));
        }
        User db = userRepository.findById(user.getId()).orElseThrow();
        if (db.getEnquiryAccessPinHash() == null || db.getEnquiryAccessPinHash().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No access code is configured. Contact a Super Admin."));
        }
        String code = body != null ? body.get("code") : null;
        if (code == null || !code.trim().matches("\\d{4}")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Enter a valid 4-digit code."));
        }
        if (!passwordEncoder.matches(code.trim(), db.getEnquiryAccessPinHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Incorrect code."));
        }
        long maxAgeSeconds = jwtService.getEnquiryUnlockExpirationMs() / 1000L;
        String token = jwtService.generateEnquiryUnlockToken(db);
        response.addHeader("Set-Cookie", buildAuthCookie("enquiryUnlock", token, maxAgeSeconds).toString());
        return ResponseEntity.ok(Map.of("unlocked", true));
    }

    // Handling logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildAuthCookie("token", "", 0).toString());
        response.addHeader("Set-Cookie", buildAuthCookie("refreshToken", "", 0).toString());
        response.addHeader("Set-Cookie", buildAuthCookie("enquiryUnlock", "", 0).toString());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("refreshToken".equals(c.getName())) {
                    refreshToken = c.getValue();
                }
            }
        }
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String newAccessToken = jwtService.generateTokenFromRefresh(refreshToken);
        response.addHeader("Set-Cookie",
                buildAuthCookie("token", newAccessToken, accessTokenExpiration / 1000).toString());
        return ResponseEntity.ok(Map.of("message", "Token refreshed"));
    }

    /**
     * Omit cookie {@code Domain} when blank so browsers use host-only cookies (needed for local dev
     * on localhost with different ports for UI and API).
     */
    private ResponseCookie buildAuthCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(httpSecure)
                .path("/")
                .sameSite(httpSecure ? "None" : "Lax")
                .maxAge(maxAgeSeconds);
        if (cookiesDomain != null && !cookiesDomain.isBlank()) {
            b = b.domain(cookiesDomain.trim());
        }
        return b.build();
    }
}
