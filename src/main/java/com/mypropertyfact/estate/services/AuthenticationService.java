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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final MasterRoleRepository masterRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${app.admin.registration-pin:}")
    private String adminRegistrationPin;

    @Transactional
    public User signup(RegisterUserDto input) {
        // Check if user with this email already exists
        if (input.getEmail() != null && userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new IllegalArgumentException("An account with this email address already exists. Please use a different email or try logging in instead.");
        }
        
        User user = new User();
        // Ensure fullName is not null - use email or default value if not provided
        String fullName = (input.getFullName() != null && !input.getFullName().trim().isEmpty()) 
                ? input.getFullName().trim() 
                : (input.getEmail() != null ? input.getEmail().split("@")[0] : "User");
        user.setFullName(fullName);
        user.setEmail(input.getEmail());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setEnabled(true);

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

        if (hasPendingAdminStaffApproval(user)) {
            throw new BadCredentialsException(
                    "Your Admin access is pending approval by a Super Admin. "
                            + "You will be able to sign in to the admin dashboard after it is approved.");
        }

        return user;
    }

    private static boolean hasPendingAdminStaffApproval(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        boolean hasActiveAdmin = user.getRoles().stream()
                .anyMatch(r -> r != null
                        && r.getRoleName() != null
                        && "ADMIN".equalsIgnoreCase(r.getRoleName())
                        && Boolean.TRUE.equals(r.getIsActive()));
        return hasActiveAdmin && Boolean.FALSE.equals(user.getAdminStaffApproved());
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
        user.setAdminStaffApproved(!hasAdmin);
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
}
