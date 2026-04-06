package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.DashboardStatsResponse;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.EnqueryRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import com.mypropertyfact.estate.services.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final EnqueryRepository enqueryRepository;
    private final UserRoleService userRoleService;

    /**
     * Counts for the admin dashboard. User and enquiry totals are only exposed to Super Admins.
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsResponse> dashboardStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).build();
        }

        long userCount = 0L;
        long enquiryCount = 0L;

        if (userRoleService.userHasRole(user.getId(), "SUPERADMIN")) {
            userCount = userRepository.count();
            enquiryCount = enqueryRepository.count();
        }

        return ResponseEntity.ok(new DashboardStatsResponse(userCount, enquiryCount));
    }
}
