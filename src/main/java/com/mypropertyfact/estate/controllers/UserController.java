package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.AdminSetPasswordRequest;
import com.mypropertyfact.estate.dtos.CreateUserBySuperAdminResponse;
import com.mypropertyfact.estate.dtos.PendingPermissionsCountResponse;
import com.mypropertyfact.estate.dtos.PendingPermissionsResponse;
import com.mypropertyfact.estate.dtos.SuperAdminCreateUserRequest;
import com.mypropertyfact.estate.dtos.SuperAdminPasswordResetDecisionRequest;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.services.AdminPasswordResetRequestService;
import com.mypropertyfact.estate.services.UserService;
import com.mypropertyfact.estate.services.UserRoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.mypropertyfact.estate.repositories.UserRepository;

import java.util.List;
import java.util.Map;

@RequestMapping("/api/v1/users")
@RestController
@Slf4j
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final AdminPasswordResetRequestService adminPasswordResetRequestService;

    @GetMapping("/me")
    public ResponseEntity<User> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Getting authenticated user");
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        // Cast to User since User implements UserDetails
        User currentUser = (User) authentication.getPrincipal();
        
        // Fetch fresh data from database to ensure all fields are present
        User dbUser = userRepository.findById(currentUser.getId()).orElse(currentUser);

        return ResponseEntity.ok(dbUser);
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(@RequestBody User updatedUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Updating user profile");
        
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = (User) authentication.getPrincipal();
        User dbUser = userRepository.findById(currentUser.getId()).orElseThrow();
        
        // Update only allowed fields (don't update email, password, or role via this endpoint)
        if (updatedUser.getFullName() != null) {
            dbUser.setFullName(updatedUser.getFullName());
        }
        if (updatedUser.getPhone() != null) {
            dbUser.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getLocation() != null) {
            dbUser.setLocation(updatedUser.getLocation());
        }
        if (updatedUser.getBio() != null) {
            dbUser.setBio(updatedUser.getBio());
        }
        if (updatedUser.getAvatar() != null) {
            dbUser.setAvatar(updatedUser.getAvatar());
        }
        if (updatedUser.getExperience() != null) {
            dbUser.setExperience(updatedUser.getExperience());
        }
        if (updatedUser.getRating() != null) {
            dbUser.setRating(updatedUser.getRating());
        }
        if (updatedUser.getTotalDeals() != null) {
            dbUser.setTotalDeals(updatedUser.getTotalDeals());
        }
        if (updatedUser.getVerified() != null) {
            dbUser.setVerified(updatedUser.getVerified());
        }
        
        User savedUser = userRepository.save(dbUser);
        
        log.info("User profile updated successfully for user: {}", savedUser.getEmail());
        
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping
    public ResponseEntity<List<User>> allUsers() {
        List <User> users = userService.allUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody SuperAdminCreateUserRequest request) {
        try {
            CreateUserBySuperAdminResponse created = userService.createUserBySuperAdmin(request);
            User u = created.getUser();
            return ResponseEntity.ok(Map.of(
                    "message", "User created successfully.",
                    "user", u,
                    "password", created.getPassword(),
                    "enquiryAccessPin", created.getEnquiryAccessPin() != null
                            ? created.getEnquiryAccessPin()
                            : "",
                    "roleNames", created.getRoleNames()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/pending-permissions")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<PendingPermissionsResponse> pendingPermissions() {
        return ResponseEntity.ok(adminPasswordResetRequestService.buildPendingPermissions());
    }

    @GetMapping("/pending-permissions-count")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<PendingPermissionsCountResponse> pendingPermissionsCount() {
        return ResponseEntity.ok(adminPasswordResetRequestService.countPending());
    }

    @PutMapping("/password-reset-requests/{id}/approve")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> approvePasswordReset(
            @PathVariable Long id,
            @RequestBody(required = false) SuperAdminPasswordResetDecisionRequest body) {
        try {
            String edited = body != null && body.getEditedPassword() != null ? body.getEditedPassword().trim() : "";
            var user = adminPasswordResetRequestService.approve(
                    id,
                    edited.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(edited));
            return ResponseEntity.ok(Map.of("message", "Password updated.", "userId", user.getId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/password-reset-requests/{id}/reject")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> rejectPasswordReset(@PathVariable Long id) {
        try {
            adminPasswordResetRequestService.reject(id);
            return ResponseEntity.ok(Map.of("message", "Password reset request rejected."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}/approve-admin-staff")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> approveAdminStaff(@PathVariable Integer id) {
        try {
            User user = userRoleService.approvePendingAdminStaff(id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}/reject-admin-staff")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> rejectAdminStaff(@PathVariable Integer id) {
        try {
            User user = userRoleService.rejectPendingAdminStaff(id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User updatedUser) {
        try {
            User user = userService.updateUser(id, updatedUser);
            log.info("User updated successfully: {}", user.getEmail());
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.warn("User update validation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error updating user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<User> activateUser(@PathVariable Integer id) {
        try {
            User user = userService.activateUser(id);
            log.info("User activated successfully: {}", user.getEmail());
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            log.error("Error activating user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<User> deactivateUser(@PathVariable Integer id) {
        try {
            User user = userService.deactivateUser(id);
            log.info("User deactivated successfully: {}", user.getEmail());
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            log.error("Error deactivating user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<User> updateUserRoles(@PathVariable Integer id, @RequestBody List<Integer> roleIds) {
        try {
            User user = userRoleService.assignRolesToUser(id, roleIds);
            log.info("User roles updated successfully: {}", user.getEmail());
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            log.error("Error updating user roles: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> setUserPassword(
            @PathVariable Integer id,
            @Valid @RequestBody AdminSetPasswordRequest request) {
        try {
            userService.setPasswordByAdmin(id, request.getNewPassword());
            log.info("Password reset by admin for user id: {}", id);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (RuntimeException e) {
            log.error("Error setting password: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> deleteUserPermanently(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User actor)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            userService.deleteUserAsSuperAdmin(id, actor.getId());
            log.info("User deleted permanently: id {}", id);
            return ResponseEntity.ok(Map.of("message", "User deleted."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
