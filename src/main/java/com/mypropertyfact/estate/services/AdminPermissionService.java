package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.security.AdminPermissionKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
     * When true (default), an ADMIN with no rows in user_admin_permissions is treated as having all CMS
     * permissions. Set {@code app.admin.permissions-legacy-full-access-when-empty=false} after migrating
     * legacy admins so empty means explicit lockout.
     */
    @Value("${app.admin.permissions-legacy-full-access-when-empty:true}")
    private boolean legacyFullAccessWhenEmpty;

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
        String want = AdminPermissionKeys.canonicalizePermissionKey(permissionKey);
        if (want == null) {
            return false;
        }
        if (userRoleService.userHasRole(user.getId(), "SUPERADMIN")) {
            return true;
        }
        if (!userRoleService.userHasRole(user.getId(), "ADMIN")) {
            return false;
        }
        Set<String> perms = user.getAdminPermissions();
        if (perms == null || perms.isEmpty()) {
            // Enquiries are never included in legacy "empty = all CMS" — must be granted explicitly.
            if (AdminPermissionKeys.MANAGE_ENQUIRIES.equals(want)) {
                return false;
            }
            return legacyFullAccessWhenEmpty;
        }
        return perms.stream()
                .filter(Objects::nonNull)
                .anyMatch(p -> want.equalsIgnoreCase(p.trim()));
    }

    public Set<String> normalizePermissions(Collection<String> raw) {
        if (raw == null) {
            return new HashSet<>();
        }
        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AdminPermissionKeys::canonicalizePermissionKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
