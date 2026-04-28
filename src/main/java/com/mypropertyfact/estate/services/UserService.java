package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.AdminPasswordResetRequestRepository;
import com.mypropertyfact.estate.repositories.PropertyListingRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import com.mypropertyfact.estate.security.AdminPermissionKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminPermissionService adminPermissionService;
    private final UserRoleService userRoleService;
    private final PropertyListingRepository propertyListingRepository;
    private final AdminPasswordResetRequestRepository adminPasswordResetRequestRepository;

    public List<User> allUsers() {
        return userRepository.findAll();
    }

    public List<User> findPendingAdminStaffApprovals() {
        return userRepository.findPendingAdminStaffApprovals();
    }

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    public User updateUser(Integer id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        if (updatedUser.getFullName() != null) {
            user.setFullName(updatedUser.getFullName());
        }
        if (updatedUser.getPhone() != null) {
            user.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getLocation() != null) {
            user.setLocation(updatedUser.getLocation());
        }
        if (updatedUser.getBio() != null) {
            user.setBio(updatedUser.getBio());
        }
        if (updatedUser.getAvatar() != null) {
            user.setAvatar(updatedUser.getAvatar());
        }
        if (updatedUser.getExperience() != null) {
            user.setExperience(updatedUser.getExperience());
        }
        if (updatedUser.getRating() != null) {
            user.setRating(updatedUser.getRating());
        }
        if (updatedUser.getTotalDeals() != null) {
            user.setTotalDeals(updatedUser.getTotalDeals());
        }
        if (updatedUser.getVerified() != null) {
            user.setVerified(updatedUser.getVerified());
        }
        if (updatedUser.getEnabled() != null) {
            user.setEnabled(updatedUser.getEnabled());
        }
        if (updatedUser.getDashboardUsername() != null) {
            String u = updatedUser.getDashboardUsername().trim();
            user.setDashboardUsername(u.isEmpty() ? null : u);
        }
        if (updatedUser.getAdminPermissions() != null) {
            if (userRoleService.userHasRole(id, "ADMIN")) {
                user.setAdminPermissions(
                        adminPermissionService.normalizePermissions(updatedUser.getAdminPermissions()));
            }
        }

        boolean wantsEnquiry = user.getAdminPermissions() != null
                && user.getAdminPermissions().stream()
                .anyMatch(p -> p != null
                        && AdminPermissionKeys.MANAGE_ENQUIRIES.equalsIgnoreCase(p.trim()));

        if (!wantsEnquiry) {
            user.setEnquiryAccessPinHash(null);
        } else {
            if (updatedUser.getEnquiryAccessPin() != null) {
                String rawPin = updatedUser.getEnquiryAccessPin().trim();
                if (!rawPin.isEmpty()) {
                    if (!rawPin.matches("\\d{4}")) {
                        throw new IllegalArgumentException("Enquiries access code must be exactly 4 digits.");
                    }
                    user.setEnquiryAccessPinHash(passwordEncoder.encode(rawPin));
                }
            }
            if (user.getEnquiryAccessPinHash() == null || user.getEnquiryAccessPinHash().isBlank()) {
                throw new IllegalArgumentException(
                        "Set a 4-digit enquiries access code when Manage enquiries is enabled.");
            }
        }

        return userRepository.save(user);
    }

    public User activateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User deactivateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setEnabled(false);
        return userRepository.save(user);
    }

    /**
     * Sets a new bcrypt password for a user (super-admin only at controller layer).
     */
    public User setPasswordByAdmin(Integer id, String rawPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    /**
     * Permanently deletes a user (Super Admin only). Clears references that would block FK constraints.
     */
    @Transactional
    public void deleteUserAsSuperAdmin(int targetUserId, int actingUserId) {
        if (targetUserId == actingUserId) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (userRoleService.userHasRole(targetUserId, "SUPERADMIN")) {
            long superCount = userRepository.countDistinctUsersHavingActiveRole("SUPERADMIN");
            if (superCount <= 1) {
                throw new IllegalArgumentException("Cannot delete the last Super Administrator account.");
            }
        }

        if (propertyListingRepository.countByUserId(targetUserId) > 0) {
            throw new IllegalArgumentException(
                    "This user has property listing(s). Remove or transfer those listings before deleting the account.");
        }

        propertyListingRepository.clearApprovedByUserId(targetUserId);
        adminPasswordResetRequestRepository.deleteByUser_Id(targetUserId);

        userRepository.delete(target);
    }
}
