package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.AdminSetPasswordRequest;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.services.UserService;
import com.mypropertyfact.estate.services.UserRoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User updatedUser) {
        try {
            User user = userService.updateUser(id, updatedUser);
            log.info("User updated successfully: {}", user.getEmail());
            return ResponseEntity.ok(user);
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
}
