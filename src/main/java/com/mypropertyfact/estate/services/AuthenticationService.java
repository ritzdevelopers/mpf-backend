package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.AdminPortalRegisterRequest;
import com.mypropertyfact.estate.dtos.LoginUserDto;
import com.mypropertyfact.estate.dtos.RegisterUserDto;
import com.mypropertyfact.estate.entities.MasterRole;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.models.ResourceNotFoundException;
import com.mypropertyfact.estate.repositories.MasterRoleRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final MasterRoleRepository masterRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SendEmailHandler sendEmailHandler;

    @Value("${app.admin.registration-pin:}")
    private String adminRegistrationPin;

    /** Consumer site / app shell origin for links in forgot-password emails (no trailing slash). */
    @Value("${app.auth.password-reset-frontend-base-url:https://mypropertyfact.in}")
    private String passwordResetFrontendBaseUrl;

    @Transactional
    public User signup(RegisterUserDto input) {
        // Check if user with this email already exists
        if (input.getEmail() != null && userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new IllegalArgumentException("An account with this email address already exists. Please use a different email or try logging in instead.");
        }
        if (input.getPhone() != null && !input.getPhone().isBlank()
                && userRepository.findByPhone(input.getPhone().trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "An account with this phone number already exists. Please sign in or use a different number.");
        }

        User user = new User();
        // Ensure fullName is not null - use email or default value if not provided
        String fullName = (input.getFullName() != null && !input.getFullName().trim().isEmpty()) 
                ? input.getFullName().trim() 
                : (input.getEmail() != null ? input.getEmail().split("@")[0] : "User");
        user.setFullName(fullName);
        user.setEmail(input.getEmail());
        if (input.getPhone() != null && !input.getPhone().trim().isEmpty()) {
            user.setPhone(input.getPhone().trim());
        }
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setEnabled(true);
        user.setUserCategory("APP_USER");

        Set<MasterRole> roles = new HashSet<>();
        addDefaultUserRole(roles);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        // Authenticate user credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        // If authentication succeeds, retrieve the user from database
        // This should always succeed if authentication passed, but handle edge case gracefully
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User account not found. Please contact support if this issue persists."
                ));

        if (isStaffDashboardUser(user)) {
            validateDashboardUsername(input.getDashboardUsername(), user);
        }

        if (isPortalApprovalPending(user)) {
            throw new BadCredentialsException(
                    "Your account is pending approval by a Super Administrator. "
                            + "You will be able to sign in after it is activated.");
        }

        return user;
    }

    /**
     * True when portal self-registration still needs Super Admin activation.
     * {@link User#hasActiveSuperAdminRole()} is exempt so the sole Super Admin cannot be locked out.
     */
    public static boolean isPortalApprovalPending(User user) {
        return user.needsPortalActivation();
    }

    private static boolean isStaffDashboardUser(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        for (MasterRole r : user.getRoles()) {
            if (r == null || !Boolean.TRUE.equals(r.getIsActive()) || r.getRoleName() == null) {
                continue;
            }
            String name = r.getRoleName();
            if ("SUPERADMIN".equalsIgnoreCase(name) || "ADMIN".equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static void validateDashboardUsername(String provided, User user) {
        String expected = user.getDashboardUsername();
        boolean superOnly = hasActiveRole(user, "SUPERADMIN") && !hasActiveRole(user, "ADMIN");

        if (expected == null || expected.isBlank()) {
            // Super Admin can sign in until a username is saved in Manage Users; Admin cannot.
            if (superOnly) {
                return;
            }
            throw new BadCredentialsException(
                    "Dashboard username is not set for this account. Ask a Super Admin to set it in Manage Users.");
        }

        if (provided == null || provided.isBlank()) {
            throw new BadCredentialsException("Dashboard username is required for admin login.");
        }
        if (!expected.trim().equals(provided.trim())) {
            throw new BadCredentialsException("Invalid dashboard username.");
        }
    }

    private static boolean hasActiveRole(User user, String roleName) {
        if (user.getRoles() == null) {
            return false;
        }
        for (MasterRole r : user.getRoles()) {
            if (r != null && Boolean.TRUE.equals(r.getIsActive()) && r.getRoleName() != null
                    && roleName.equalsIgnoreCase(r.getRoleName())) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public User signupWithoutPassword(RegisterUserDto dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        // Ensure fullName is not null - use email or default value if not provided
        String fullName = (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) 
                ? dto.getFullName().trim() 
                : (dto.getEmail() != null ? dto.getEmail().split("@")[0] : "User");
        user.setFullName(fullName);
        
        // Set default USER role
        Set<MasterRole> roles = new HashSet<>();
        String roleName = (dto.getRole() != null && !dto.getRole().isEmpty()) 
                ? dto.getRole().replace("ROLE_", "") 
                : "USER";
        
        masterRoleRepository.findByRoleNameIgnoreCase(roleName)
                .ifPresentOrElse(
                        roles::add,
                        () -> {
                            // Create role if it doesn't exist
                            MasterRole role = new MasterRole();
                            role.setRoleName(roleName);
                            role.setDescription("User role");
                            role.setIsActive(true);
                            roles.add(masterRoleRepository.save(role));
                        }
                );
        user.setRoles(roles);
        user.setEnabled(true);
        user.setVerified(true);
        user.setAdminStaffApproved(true);
        user.setUserCategory("APP_USER");

        // Generate a secure random password (never used for login)
        String randomPassword = UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(randomPassword));
        userRepository.save(user);
        return user;
    }

    public Map<String, Object> getAdminRegisterMeta() {
        boolean requiresPin = adminRegistrationPin != null && !adminRegistrationPin.isBlank();
        List<Map<String, Object>> roles = new ArrayList<>();
        masterRoleRepository.findByRoleNameIgnoreCase("USER")
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .ifPresent(r -> roles.add(roleMap(r)));
        masterRoleRepository.findByRoleNameIgnoreCase("ADMIN")
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .ifPresent(r -> roles.add(roleMap(r)));
        return Map.of(
                "requiresPin", requiresPin,
                "roles", roles,
                "defaultRoleName", "USER");
    }

    private static Map<String, Object> roleMap(MasterRole r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("roleName", r.getRoleName());
        return m;
    }

    /**
     * Admin portal self-registration: defaults to USER only when {@code roleIds} is null or empty.
     * Optional extra roles: ADMIN (requires dashboard username). SUPERADMIN is not allowed.
     */
    @Transactional
    public User registerAdminPortalUser(AdminPortalRegisterRequest req) {
        if (adminRegistrationPin != null && !adminRegistrationPin.isBlank()) {
            if (req.getRegistrationPin() == null
                    || !adminRegistrationPin.trim().equals(req.getRegistrationPin().trim())) {
                throw new IllegalArgumentException("Invalid or missing registration PIN.");
            }
        }

        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    "An account with this email already exists. Try signing in instead.");
        }

        Set<MasterRole> roles = new HashSet<>();
        boolean hasAdmin = false;

        List<Integer> ids = req.getRoleIds();
        if (ids == null || ids.isEmpty()) {
            addDefaultUserRole(roles);
        } else {
            Set<Integer> seen = new HashSet<>();
            for (Integer rid : ids) {
                if (rid == null || !seen.add(rid)) {
                    continue;
                }
                MasterRole r = masterRoleRepository.findById(rid)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid role selected."));
                if (!Boolean.TRUE.equals(r.getIsActive())) {
                    throw new IllegalArgumentException("Invalid role selected.");
                }
                String n = r.getRoleName();
                if (n == null) {
                    continue;
                }
                if ("SUPERADMIN".equalsIgnoreCase(n)) {
                    throw new IllegalArgumentException("Super Admin cannot be created from this form.");
                }
                if (!"USER".equalsIgnoreCase(n) && !"ADMIN".equalsIgnoreCase(n)) {
                    throw new IllegalArgumentException("Only USER and ADMIN roles are allowed here.");
                }
                if ("ADMIN".equalsIgnoreCase(n)) {
                    hasAdmin = true;
                }
                roles.add(r);
            }
            if (roles.isEmpty()) {
                addDefaultUserRole(roles);
            } else if (!roles.stream().anyMatch(r -> r.getRoleName() != null
                    && "USER".equalsIgnoreCase(r.getRoleName()))) {
                addDefaultUserRole(roles);
            }
            hasAdmin = roles.stream().anyMatch(r -> r.getRoleName() != null
                    && "ADMIN".equalsIgnoreCase(r.getRoleName()));
        }

        String dash = req.getDashboardUsername() != null ? req.getDashboardUsername().trim() : "";
        if (hasAdmin) {
            if (dash.isEmpty()) {
                throw new IllegalArgumentException("Dashboard username is required when Admin role is selected.");
            }
            if (userRepository.findByDashboardUsername(dash).isPresent()) {
                throw new IllegalArgumentException("That dashboard username is already taken.");
            }
        }

        User user = new User();
        String fullName = req.getFullName() != null ? req.getFullName().trim() : "";
        if (fullName.isEmpty()) {
            fullName = email.contains("@") ? email.substring(0, email.indexOf('@')) : "User";
        }
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setDashboardUsername(hasAdmin ? dash : (dash.isEmpty() ? null : dash));
        user.setRoles(roles);
        user.setEnabled(true);
        user.setVerified(true);
        user.setAdminStaffApproved(false);
        user.setUserCategory("ADMIN_USER");
        return userRepository.save(user);
    }

    private void addDefaultUserRole(Set<MasterRole> roles) {
        masterRoleRepository.findByRoleNameIgnoreCase("USER")
                .ifPresentOrElse(
                        roles::add,
                        () -> {
                            MasterRole userRole = new MasterRole();
                            userRole.setRoleName("USER");
                            userRole.setDescription("Default user role");
                            userRole.setIsActive(true);
                            roles.add(masterRoleRepository.save(userRole));
                        });
    }

    /**
     * Sends a reset link when the email belongs to an account (same generic outcome when missing — no enumeration).
     */
    public void sendConsumerPasswordResetEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return;
        }
        String email = rawEmail.trim();
        Optional<User> opt = userRepository.findByEmailIgnoreCase(email);
        if (opt.isEmpty()) {
            return;
        }
        User user = opt.get();
        String accountEmail = user.getEmail();
        if (accountEmail == null || accountEmail.isBlank()) {
            return;
        }

        String resetJwt = jwtService.generatePasswordResetToken(user);
        String resetUrl = JwtService.buildPasswordResetLink(passwordResetFrontendBaseUrl, resetJwt);

        String body = """
                <!DOCTYPE html>
                <html>
                <head>
                <style>
                body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                .container { max-width: 600px; background: #ffffff; padding: 20px; border-radius: 8px;
                  box-shadow: 0 0 10px rgba(0,0,0,0.1); }
                .header { font-size: 20px; font-weight: bold; color: #2c3e50; margin-bottom: 12px; }
                .message { font-size: 14px; color: #555; line-height: 1.6; }
                .button {
                  display: inline-block; margin: 18px 0; padding: 12px 20px;
                  background: #2563eb; color: #fff !important; text-decoration: none; border-radius: 6px; font-weight: bold;
                }
                .footer { margin-top: 20px; font-size: 13px; color: #777; }
                </style>
                </head>
                <body>
                <div class="container">
                <div class="header">Reset your password</div>
                <p class="message">
                Hello,<br><br>
                We received a request to reset the password for your My Property Fact account.
                Click the button below to choose a new password. This link will expire shortly.
                </p>
                <p><a class="button" href="%s">Reset password</a></p>
                <p class="message">
                If you did not request this, you can safely ignore this email.
                </p>
                <div class="footer">Regards,<br><strong>My Property Fact Team</strong></div>
                </div>
                </body>
                </html>
                """.formatted(resetUrl);
        sendEmailHandler.sendEmail(accountEmail, "Reset your My Property Fact password", body);
    }

    @Transactional
    public void resetPasswordWithConsumerToken(String token, String newPassword) {
        JwtService.PasswordResetTokenPayload payload = jwtService.parsePasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired reset link. Please request a new password reset."));

        User user = userRepository.findByEmailIgnoreCase(payload.email())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired reset link. Please request a new password reset."));

        int expectedTv = payload.tokenVersion();
        int currentTv = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        if (expectedTv != currentTv) {
            throw new IllegalArgumentException(
                    "This reset link is no longer valid. Please request a new password reset.");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setTokenVersion(currentTv + 1);
        userRepository.save(user);
    }
}
