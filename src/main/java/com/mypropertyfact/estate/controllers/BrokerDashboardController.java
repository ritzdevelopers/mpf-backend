package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.BrokerDashboardStatsResponse;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.services.BrokerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class BrokerDashboardController {

    private final BrokerDashboardService brokerDashboardService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<BrokerDashboardStatsResponse> dashboardStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(brokerDashboardService.getStatsForUser(user.getId()));
    }
}
