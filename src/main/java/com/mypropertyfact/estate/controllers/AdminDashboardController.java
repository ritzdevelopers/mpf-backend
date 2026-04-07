package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.AdminDashboardActivityItemDto;
import com.mypropertyfact.estate.dtos.DashboardStatsResponse;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.EnqueryRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import com.mypropertyfact.estate.services.AdminDashboardActivityService;
import com.mypropertyfact.estate.services.EnquiryAccessService;
import com.mypropertyfact.estate.services.UserRoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final EnqueryRepository enqueryRepository;
    private final UserRoleService userRoleService;
    private final EnquiryAccessService enquiryAccessService;
    private final AdminDashboardActivityService adminDashboardActivityService;

    /**
     * Counts for the admin dashboard. User total: Super Admins only.
     * Enquiry total: Super Admins, or Admins with enquiries access (permission + unlock cookie).
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsResponse> dashboardStats(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).build();
        }

        long userCount = 0L;
        long enquiryCount = 0L;

        if (userRoleService.userHasRole(user.getId(), "SUPERADMIN")) {
            userCount = userRepository.count();
            enquiryCount = enqueryRepository.count();
        } else if (enquiryAccessService.canAccessEnquiries(user, request)) {
            enquiryCount = enqueryRepository.count();
        }

        return ResponseEntity.ok(new DashboardStatsResponse(userCount, enquiryCount));
    }

    /**
     * Unified “recent tasks” for the signed-in admin (blogs, web stories, categories, property reviews).
     */
    @GetMapping("/dashboard/my-activity")
    public ResponseEntity<?> myRecentActivity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).build();
        }
        if (!userRoleService.userHasRole(user.getId(), "SUPERADMIN")
                && !userRoleService.userHasRole(user.getId(), "ADMIN")) {
            return ResponseEntity.status(403).build();
        }

        List<AdminDashboardActivityItemDto> items =
                adminDashboardActivityService.getRecentForUser(user.getId(), 20);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("activities", items);
        return ResponseEntity.ok(body);
    }
}
