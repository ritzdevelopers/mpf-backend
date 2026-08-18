package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.AdminManagementActivityPageResponse;
import com.mypropertyfact.estate.services.AdminManagementActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/management")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminManagementController {

    private final AdminManagementActivityService adminManagementActivityService;

    /**
     * Paginated activity feed for the admin “Activity log” page (formatted audit entries).
     * Date filters accept {@code YYYY-MM-DD} (full local day) or ISO date-times.
     */
    @GetMapping("/activities")
    public ResponseEntity<AdminManagementActivityPageResponse> activities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String kind) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        AdminManagementActivityPageResponse body = adminManagementActivityService.list(
                from,
                to,
                q,
                kind,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt")));
        return ResponseEntity.ok(body);
    }
}
