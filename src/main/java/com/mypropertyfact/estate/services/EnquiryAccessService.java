package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.security.AdminPermissionKeys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnquiryAccessService {

    private final UserRoleService userRoleService;
    private final AdminPermissionService adminPermissionService;
    private final JwtService jwtService;

    public static String readEnquiryUnlockCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie c : request.getCookies()) {
            if ("enquiryUnlock".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    /** Super Admin always; otherwise MANAGE_ENQUIRIES plus valid short-lived enquiry-unlock cookie. */
    public boolean canAccessEnquiries(User user, HttpServletRequest request) {
        if (user == null || request == null) {
            return false;
        }
        if (userRoleService.userHasRole(user.getId(), "SUPERADMIN")) {
            return true;
        }
        if (!adminPermissionService.can(user, AdminPermissionKeys.MANAGE_ENQUIRIES)) {
            return false;
        }
        String token = readEnquiryUnlockCookie(request);
        return jwtService.isEnquiryUnlockTokenValid(token, user.getEmail());
    }
}
