package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.AdminPasswordResetSubmitRequest;
import com.mypropertyfact.estate.dtos.PasswordResetPendingRowDto;
import com.mypropertyfact.estate.dtos.PendingPermissionsCountResponse;
import com.mypropertyfact.estate.dtos.PendingPermissionsResponse;
import com.mypropertyfact.estate.entities.AdminPasswordResetRequest;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.enums.AdminPasswordResetStatus;
import com.mypropertyfact.estate.repositories.AdminPasswordResetRequestRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPasswordResetRequestService {

    private final AdminPasswordResetRequestRepository resetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    private static boolean hasStaffDashboardRole(User u) {
        if (u.getRoles() == null) {
            return false;
        }
        return u.getRoles().stream()
                .anyMatch(r -> r != null
                        && Boolean.TRUE.equals(r.getIsActive())
                        && r.getRoleName() != null
                        && ("ADMIN".equalsIgnoreCase(r.getRoleName())
                                || "SUPERADMIN".equalsIgnoreCase(r.getRoleName())));
    }

    /**
     * Step 1 of forgot-password: ensure the email belongs to an admin-dashboard account.
     */
    @Transactional(readOnly = true)
    public void checkEmailEligibleForPasswordReset(String emailRaw) {
        if (emailRaw == null || emailRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String email = emailRaw.trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found for this email."));
        if (!hasStaffDashboardRole(user)) {
            throw new IllegalArgumentException("This flow is only for admin dashboard accounts.");
        }
    }

    @Transactional
    public void submitRequest(AdminPasswordResetSubmitRequest body) {
        if (!body.getNewPassword().equals(body.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match.");
        }
        String email = body.getEmail().trim().toLowerCase();
        String dashUser = body.getDashboardUsername().trim();
        if (email.isEmpty() || dashUser.isEmpty()) {
            throw new IllegalArgumentException("Email and dashboard username are required.");
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found for this email."));
        if (user.getDashboardUsername() == null
                || !user.getDashboardUsername().trim().equalsIgnoreCase(dashUser)) {
            throw new IllegalArgumentException("Dashboard username does not match this email.");
        }
        if (!hasStaffDashboardRole(user)) {
            throw new IllegalArgumentException("This flow is only for admin dashboard accounts.");
        }

        resetRepository.deleteByUser_IdAndStatus(user.getId(), AdminPasswordResetStatus.PENDING);

        AdminPasswordResetRequest row = new AdminPasswordResetRequest();
        row.setUser(user);
        row.setProposedPasswordHash(passwordEncoder.encode(body.getNewPassword()));
        row.setStatus(AdminPasswordResetStatus.PENDING);
        resetRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<PasswordResetPendingRowDto> listPendingRows() {
        return resetRepository.findByStatusOrderByCreatedAtDesc(AdminPasswordResetStatus.PENDING).stream()
                .map(r -> {
                    User u = r.getUser();
                    return PasswordResetPendingRowDto.builder()
                            .id(r.getId())
                            .userId(u.getId())
                            .email(u.getEmail())
                            .fullName(u.getFullName())
                            .dashboardUsername(u.getDashboardUsername())
                            .requestedAt(r.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PendingPermissionsResponse buildPendingPermissions() {
        return PendingPermissionsResponse.builder()
                .adminAccessRequests(userService.findPendingPortalApprovals())
                .passwordChangeRequests(listPendingRows())
                .build();
    }

    @Transactional(readOnly = true)
    public PendingPermissionsCountResponse countPending() {
        int pw = (int) resetRepository.countByStatus(AdminPasswordResetStatus.PENDING);
        int admin = userService.findPendingPortalApprovals().size();
        return PendingPermissionsCountResponse.builder()
                .adminAccessPending(admin)
                .passwordChangePending(pw)
                .totalPending(admin + pw)
                .build();
    }

    @Transactional
    public User approve(Long requestId, Optional<String> editedPlainPasswordOptional) {
        AdminPasswordResetRequest req = resetRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (req.getStatus() != AdminPasswordResetStatus.PENDING) {
            throw new IllegalArgumentException("This request is no longer pending.");
        }
        User user = userRepository.findById(req.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String toApply;
        if (editedPlainPasswordOptional.isPresent() && !editedPlainPasswordOptional.get().isBlank()) {
            String p = editedPlainPasswordOptional.get().trim();
            if (p.length() < 8) {
                throw new IllegalArgumentException("Edited password must be at least 8 characters.");
            }
            toApply = passwordEncoder.encode(p);
        } else {
            toApply = req.getProposedPasswordHash();
        }
        user.setPassword(toApply);
        int v = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        user.setTokenVersion(v + 1);
        userRepository.save(user);

        req.setStatus(AdminPasswordResetStatus.APPROVED);
        req.setResolvedAt(LocalDateTime.now());
        resetRepository.save(req);
        return user;
    }

    @Transactional
    public void reject(Long requestId) {
        AdminPasswordResetRequest req = resetRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (req.getStatus() != AdminPasswordResetStatus.PENDING) {
            throw new IllegalArgumentException("This request is no longer pending.");
        }
        req.setStatus(AdminPasswordResetStatus.REJECTED);
        req.setResolvedAt(LocalDateTime.now());
        resetRepository.save(req);
    }
}
