package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.AdminDashboardActivityItemDto;
import com.mypropertyfact.estate.dtos.DashboardStatsResponse;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.EnqueryRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import com.mypropertyfact.estate.services.AdminDashboardActivityService;
import com.mypropertyfact.estate.services.EnquiryAccessService;
import com.mypropertyfact.estate.services.SiteTrafficService;
import com.mypropertyfact.estate.services.UserRoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final com.mypropertyfact.estate.repositories.ProjectRepository projectRepository;
    private final com.mypropertyfact.estate.repositories.BlogRepository blogRepository;
    private final com.mypropertyfact.estate.repositories.BlogCategoryRepository blogCategoryRepository;
    private final com.mypropertyfact.estate.repositories.CityRepository cityRepository;
    private final com.mypropertyfact.estate.repositories.BuilderRepository builderRepository;
    private final com.mypropertyfact.estate.repositories.AmenityRepository amenityRepository;
    private final com.mypropertyfact.estate.repositories.WebStoryCategoryRepository webStoryCategoryRepository;
    private final com.mypropertyfact.estate.repositories.WebStoryRepository webStoryRepository;
    private final com.mypropertyfact.estate.repositories.ProjectTypeRepository projectTypeRepository;
    private final UserRoleService userRoleService;
    private final EnquiryAccessService enquiryAccessService;
    private final AdminDashboardActivityService adminDashboardActivityService;
    private final SiteTrafficService siteTrafficService;

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
        long projectCount = 0L;
        long blogCount = 0L;
        long blogCategoryCount = 0L;
        long cityCount = 0L;
        long builderCount = 0L;
        long amenityCount = 0L;
        long webStoryCategoryCount = 0L;
        long webStoryCount = 0L;
        long projectTypeCount = 0L;

        if (userRoleService.userHasRole(user.getId(), "SUPERADMIN")) {
            userCount = userRepository.count();
            enquiryCount = enqueryRepository.count();
            projectCount = projectRepository.count();
            blogCount = blogRepository.count();
            blogCategoryCount = blogCategoryRepository.count();
            cityCount = cityRepository.count();
            builderCount = builderRepository.count();
            amenityCount = amenityRepository.count();
            webStoryCategoryCount = webStoryCategoryRepository.count();
            webStoryCount = webStoryRepository.count();
            projectTypeCount = projectTypeRepository.count();
        } else {
            if (enquiryAccessService.canAccessEnquiries(user, request)) {
                enquiryCount = enqueryRepository.count();
            }
            if (userRoleService.userHasRole(user.getId(), "ADMIN")) {
                // Admins typically have access to some or all of these depending on finer permissions.
                // For the aggregated dashboard stats endpoint we will return the global counts and the UI will gate them.
                projectCount = projectRepository.count();
                blogCount = blogRepository.count();
                blogCategoryCount = blogCategoryRepository.count();
                cityCount = cityRepository.count();
                builderCount = builderRepository.count();
                amenityCount = amenityRepository.count();
                webStoryCategoryCount = webStoryCategoryRepository.count();
                webStoryCount = webStoryRepository.count();
                projectTypeCount = projectTypeRepository.count();
            }
        }

        return ResponseEntity.ok(new DashboardStatsResponse(
                userCount, enquiryCount, projectCount, blogCount, blogCategoryCount, cityCount, builderCount, amenityCount, webStoryCategoryCount, webStoryCount, projectTypeCount
        ));
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

    /**
     * Aggregated public-site traffic by day for the dashboard chart (no visitor IPs).
     * Available to Super Admins and Admins (same visibility as the chart card).
     */
    @GetMapping("/dashboard/site-traffic-trends")
    public ResponseEntity<?> siteTrafficTrends(@RequestParam(defaultValue = "14") int days) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).build();
        }
        if (!userRoleService.userHasRole(user.getId(), "SUPERADMIN")
                && !userRoleService.userHasRole(user.getId(), "ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(siteTrafficService.buildDailyTrendForDashboard(days));
    }
}
