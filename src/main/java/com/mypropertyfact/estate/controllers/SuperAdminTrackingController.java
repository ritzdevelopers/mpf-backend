package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.AdminAuditLogEntryDto;
import com.mypropertyfact.estate.dtos.AdminAuditLogPageResponse;
import com.mypropertyfact.estate.dtos.SiteTrafficSummaryResponse;
import com.mypropertyfact.estate.dtos.SiteTrafficVisitPageResponse;
import com.mypropertyfact.estate.dtos.TrafficRevealRequest;
import com.mypropertyfact.estate.services.AdminAuditLogService;
import com.mypropertyfact.estate.services.SiteTrafficService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/super")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuperAdminTrackingController {

    private final SiteTrafficService siteTrafficService;
    private final AdminAuditLogService adminAuditLogService;

    @Value("${http.secure}")
    private boolean httpSecure;

    @Value("${cookies.domain:}")
    private String cookiesDomain;

    @Value("${mpf.traffic-reveal-pin:2026}")
    private String trafficRevealPin;

    @GetMapping("/traffic/summary")
    public ResponseEntity<SiteTrafficSummaryResponse> trafficSummary() {
        return ResponseEntity.ok(siteTrafficService.buildSummary());
    }

    @GetMapping("/traffic/reveal-status")
    public ResponseEntity<Map<String, Object>> trafficRevealStatus(HttpServletRequest request) {
        boolean revealed = SiteTrafficService.hasTrafficRevealCookie(request);
        return ResponseEntity.ok(Map.of(
                "revealed", revealed,
                "requiresPin", true));
    }

    @PostMapping("/traffic/reveal")
    public ResponseEntity<?> trafficReveal(
            @RequestBody(required = false) TrafficRevealRequest body,
            HttpServletResponse response) {
        String pin = body != null && body.getPin() != null ? body.getPin().trim() : "";
        String expected = trafficRevealPin != null ? trafficRevealPin.trim() : "2026";
        if (!expected.equals(pin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "invalid_pin"));
        }
        response.addHeader("Set-Cookie", buildRevealCookie(8 * 3600).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/traffic/visits")
    public ResponseEntity<SiteTrafficVisitPageResponse> trafficVisits(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(siteTrafficService.listRecentVisits(request, page, size));
    }

    /**
     * CSV export of public-site traffic events in the last N hours (1–168). Real IPs only if the
     * traffic-reveal cookie is set (same as on-screen table).
     */
    @GetMapping(value = "/traffic/visits-export", produces = "text/csv")
    public ResponseEntity<byte[]> trafficVisitsExport(
            HttpServletRequest request,
            @RequestParam(defaultValue = "24") int hours) {
        int safeHours = Math.min(Math.max(hours, 1), 168);
        if (!siteTrafficService.hasExportWindowHistory(safeHours)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Export is available only after " + safeHours + " hours of collected traffic data.")
                            .getBytes(StandardCharsets.UTF_8));
        }
        byte[] csv = siteTrafficService.buildVisitsCsvExport(request, safeHours);
        String filename = "mpf-public-traffic-" + safeHours + "h.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<AdminAuditLogPageResponse> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String pathContains) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<AdminAuditLogEntryDto> result = adminAuditLogService.search(
                from,
                to,
                email,
                success,
                pathContains,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt")));
        AdminAuditLogPageResponse body = AdminAuditLogPageResponse.builder()
                .content(result.getContent())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .number(result.getNumber())
                .size(result.getSize())
                .build();
        return ResponseEntity.ok(body);
    }

    /**
     * Same rules as auth cookies: omit Domain when blank for localhost cross-port dev.
     */
    private ResponseCookie buildRevealCookie(long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(
                        SiteTrafficService.TRAFFIC_REVEAL_COOKIE,
                        SiteTrafficService.TRAFFIC_REVEAL_VALUE)
                .httpOnly(true)
                .secure(httpSecure)
                .path("/")
                .sameSite(httpSecure ? "None" : "Lax")
                .maxAge(maxAgeSeconds);
        if (cookiesDomain != null && !cookiesDomain.isBlank()) {
            b = b.domain(cookiesDomain.trim());
        }
        return b.build();
    }
}
