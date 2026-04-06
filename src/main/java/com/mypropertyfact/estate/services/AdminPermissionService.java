package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.security.AdminPermissionKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service("adminPermissionService")
@RequiredArgsConstructor
public class AdminPermissionService {

    private final UserRoleService userRoleService;

    /**
     * For {@code @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_BLOGS')")}.
     */
    public boolean can(Authentication authentication, String permissionKey) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return can(user, permissionKey);
    }

    public boolean can(User user, String permissionKey) {
        if (user == null || permissionKey == null || permissionKey.isBlank()) {
            return false;
        }
        if (!AdminPermissionKeys.allKeys().contains(permissionKey)) {
            return false;
        }
        if (userRoleService.userHasRole(user.getId(), "SUPERADMIN")) {
            return true;
        }
        if (!userRoleService.userHasRole(user.getId(), "ADMIN")) {
            return false;
        }
        Set<String> perms = user.getAdminPermissions();
        return perms != null && perms.contains(permissionKey);
    }

    public Set<String> normalizePermissions(Collection<String> raw) {
        if (raw == null) {
            return new HashSet<>();
        }
        Set<String> allowed = AdminPermissionKeys.allKeys();
        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(allowed::contains)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
