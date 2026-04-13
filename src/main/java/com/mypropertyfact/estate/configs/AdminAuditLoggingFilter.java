package com.mypropertyfact.estate.configs;

import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.services.AdminApiTaskDescriber;
import com.mypropertyfact.estate.services.AdminAuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class AdminAuditLoggingFilter extends OncePerRequestFilter {

    private static final String ADMIN_API_PREFIX = "/api/v1/admin";
    static final String HEADER_ADMIN_PAGE = "X-MPF-Admin-Page";
    static final String HEADER_DWELL_MS = "X-MPF-Dwell-Ms";

    private final AdminAuditLogService adminAuditLogService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!isAdminApiPath(request) || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (shouldSkipAudit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                int status = response.getStatus();
                long durationMs = (System.nanoTime() - start) / 1_000_000L;
                String pathOnly = stripContextPath(request.getRequestURI(), request.getContextPath());
                String query = request.getQueryString();
                if (query != null && query.isEmpty()) {
                    query = null;
                }
                String actorEmail = user.getEmail() != null && !user.getEmail().isBlank()
                        ? user.getEmail()
                        : user.getUsername();
                String taskLabel = AdminApiTaskDescriber.describe(request.getMethod(), pathOnly);
                String clientPage = readClientAdminPage(request);
                Integer dwellMs = readClientDwellMs(request);
                adminAuditLogService.record(
                        actorEmail,
                        user.getId(),
                        request.getMethod(),
                        pathOnly,
                        query,
                        status > 0 ? status : 200,
                        (int) Math.min(durationMs, Integer.MAX_VALUE),
                        taskLabel,
                        clientPage,
                        dwellMs);
            } catch (Exception e) {
                log.warn("Admin audit log failed: {}", e.getMessage());
            }
        }
    }

    private static boolean isAdminApiPath(HttpServletRequest request) {
        String path = stripContextPath(request.getRequestURI(), request.getContextPath());
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        return path.startsWith(ADMIN_API_PREFIX);
    }

    private static boolean shouldSkipAudit(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String path = stripContextPath(request.getRequestURI(), request.getContextPath());
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        return path.startsWith("/api/v1/admin/super/traffic/summary")
                || path.startsWith("/api/v1/admin/super/traffic/visits")
                || path.startsWith("/api/v1/admin/super/traffic/reveal-status")
                || path.startsWith("/api/v1/admin/super/audit-logs")
                || path.startsWith("/api/v1/admin/dashboard/site-traffic-trends")
                || path.startsWith("/api/v1/admin/dashboard/site-traffic-live")
                || path.startsWith("/api/v1/admin/dashboard/site-traffic-today");
    }

    private static String stripContextPath(String uri, String contextPath) {
        if (uri == null) {
            return "";
        }
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private static String readClientAdminPage(HttpServletRequest request) {
        String raw = request.getHeader(HEADER_ADMIN_PAGE);
        if (raw == null) {
            return null;
        }
        String p = raw.trim();
        if (p.isEmpty()) {
            return null;
        }
        return p.length() > AdminAuditLogService.CLIENT_PAGE_MAX
                ? p.substring(0, AdminAuditLogService.CLIENT_PAGE_MAX)
                : p;
    }

    private static Integer readClientDwellMs(HttpServletRequest request) {
        String raw = request.getHeader(HEADER_DWELL_MS);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long n = Long.parseLong(raw.trim());
            if (n < 0 || n > 86_400_000L) {
                return null;
            }
            return (int) Math.min(n, Integer.MAX_VALUE);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
