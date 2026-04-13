package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.SiteTrafficDailyBucketDto;
import com.mypropertyfact.estate.dtos.SiteTrafficDailyTrendResponse;
import com.mypropertyfact.estate.dtos.SiteTrafficPathCountDto;
import com.mypropertyfact.estate.dtos.SiteTrafficSummaryResponse;
import com.mypropertyfact.estate.dtos.SiteTrafficVisitDto;
import com.mypropertyfact.estate.dtos.SiteTrafficVisitPageResponse;
import com.mypropertyfact.estate.entities.SiteTrafficEvent;
import com.mypropertyfact.estate.repositories.SiteTrafficEventRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SiteTrafficService {

    public static final int PATH_MAX_LEN = 512;
    public static final int SESSION_ID_MAX_LEN = 64;
    public static final int REMOTE_ADDR_MAX_LEN = 64;
    public static final String TRAFFIC_REVEAL_COOKIE = "mpfTrafficReveal";
    public static final String TRAFFIC_REVEAL_VALUE = "1";
    private static final long PING_THROTTLE_MS = 60_000L;
    private static final long MAX_DWELL_MS = 86_400_000L;
    /** Drop only very short positive segments (noise); 0ms completed navigations are still stored. */
    private static final int MIN_DWELL_TO_PERSIST_MS = 50;

    private final SiteTrafficEventRepository siteTrafficEventRepository;

    private final ConcurrentHashMap<String, Long> lastPingByKey = new ConcurrentHashMap<>();

    public String validateAndNormalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String p = raw.trim();
        if (p.length() > PATH_MAX_LEN) {
            p = p.substring(0, PATH_MAX_LEN);
        }
        if (!p.startsWith("/")) {
            return null;
        }
        if (p.contains("..") || p.contains("\\") || p.contains("\0")) {
            return null;
        }
        int q = p.indexOf('?');
        if (q >= 0) {
            p = p.substring(0, q);
        }
        if (p.isBlank() || "/".equals(p)) {
            return "/";
        }
        return p;
    }

    /**
     * Records a public page visit. If {@code dwellMs} is present (including 0), stores a completed visit
     * (duration on that path). Otherwise applies a light throttle and records a simple ping (legacy).
     */
    @Transactional
    public void recordVisit(String path, Long dwellMs, String clientSessionId, HttpServletRequest request) {
        String ip = truncate(resolveClientIp(request), REMOTE_ADDR_MAX_LEN);
        String sessionPart = truncate(safeSessionId(clientSessionId), SESSION_ID_MAX_LEN);

        if (dwellMs != null) {
            long capped = Math.min(Math.max(dwellMs, 0), MAX_DWELL_MS);
            if (capped > 0 && capped < MIN_DWELL_TO_PERSIST_MS) {
                return;
            }
            SiteTrafficEvent e = new SiteTrafficEvent();
            e.setPath(path);
            e.setClientSessionId(sessionPart.isEmpty() ? null : sessionPart);
            e.setRemoteAddr(ip.isEmpty() ? null : ip);
            e.setDwellMs((int) Math.min(capped, Integer.MAX_VALUE));
            siteTrafficEventRepository.save(e);
            return;
        }

        String key = ip + "|" + path;
        long now = System.currentTimeMillis();
        Long last = lastPingByKey.get(key);
        if (last != null && now - last < PING_THROTTLE_MS) {
            return;
        }
        lastPingByKey.put(key, now);

        SiteTrafficEvent e = new SiteTrafficEvent();
        e.setPath(path);
        e.setClientSessionId(sessionPart.isEmpty() ? null : sessionPart);
        e.setRemoteAddr(ip.isEmpty() ? null : ip);
        e.setDwellMs(null);
        siteTrafficEventRepository.save(e);
    }

    public static boolean hasTrafficRevealCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        for (Cookie c : request.getCookies()) {
            if (TRAFFIC_REVEAL_COOKIE.equals(c.getName()) && TRAFFIC_REVEAL_VALUE.equals(c.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public SiteTrafficVisitPageResponse listRecentVisits(
            HttpServletRequest request, int page, int size) {
        boolean reveal = hasTrafficRevealCookie(request);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        LocalDateTime since = LocalDateTime.now().minusHours(48);
        Page<SiteTrafficEvent> p = siteTrafficEventRepository.findByOccurredAtAfterOrderByOccurredAtDesc(
                since, PageRequest.of(safePage, safeSize));

        List<SiteTrafficVisitDto> rows = new ArrayList<>();
        for (SiteTrafficEvent e : p.getContent()) {
            String ip = e.getRemoteAddr();
            rows.add(SiteTrafficVisitDto.builder()
                    .id(e.getId())
                    .occurredAt(e.getOccurredAt())
                    .path(e.getPath())
                    .dwellMs(e.getDwellMs())
                    .clientIp(reveal ? (ip != null ? ip : "—") : maskIp(ip))
                    .ipRevealed(reveal)
                    .build());
        }

        return SiteTrafficVisitPageResponse.builder()
                .content(rows)
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .number(p.getNumber())
                .size(p.getSize())
                .ipRevealActive(reveal)
                .build();
    }

    private static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "—";
        }
        return "Unlock to view IP";
    }

    /**
     * Daily public-site traffic counts for the admin dashboard chart (aggregates only, no IPs).
     * Compares the last 7 calendar days to the 7 days before that in the JVM default timezone.
     */
    @Transactional(readOnly = true)
    public SiteTrafficDailyTrendResponse buildDailyTrendForDashboard(int requestedDays) {
        int days = Math.min(Math.max(requestedDays, 7), 31);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(days - 1L);

        List<SiteTrafficDailyBucketDto> buckets = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            LocalDateTime from = d.atStartOfDay();
            LocalDateTime to = d.plusDays(1).atStartOfDay();
            long c = siteTrafficEventRepository.countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(from, to);
            buckets.add(SiteTrafficDailyBucketDto.builder()
                    .date(d.toString())
                    .count(c)
                    .build());
        }

        LocalDateTime last7From = today.minusDays(6).atStartOfDay();
        LocalDateTime last7To = today.plusDays(1).atStartOfDay();
        long visitsLast7 = siteTrafficEventRepository.countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                last7From, last7To);

        LocalDateTime prior7From = today.minusDays(13).atStartOfDay();
        LocalDateTime prior7To = today.minusDays(6).atStartOfDay();
        long visitsPrior7 = siteTrafficEventRepository.countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                prior7From, prior7To);

        Double pct = null;
        if (visitsPrior7 > 0) {
            pct = (visitsLast7 - visitsPrior7) / (double) visitsPrior7 * 100.0;
        }

        return SiteTrafficDailyTrendResponse.builder()
                .dailyBuckets(buckets)
                .visitsLast7Days(visitsLast7)
                .visitsPrior7Days(visitsPrior7)
                .percentChangeVsPrior7Days(pct)
                .build();
    }

    @Transactional(readOnly = true)
    public SiteTrafficSummaryResponse buildSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime t15 = now.minusMinutes(15);
        LocalDateTime t60 = now.minusHours(1);
        LocalDateTime t24 = now.minusHours(24);

        long c15 = siteTrafficEventRepository.countByOccurredAtAfter(t15);
        long c60 = siteTrafficEventRepository.countByOccurredAtAfter(t60);
        long c24 = siteTrafficEventRepository.countByOccurredAtAfter(t24);

        List<Object[]> rawTop = siteTrafficEventRepository.countGroupedByPathSince(
                t24, PageRequest.of(0, 25));
        List<SiteTrafficPathCountDto> top = new ArrayList<>();
        for (Object[] row : rawTop) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            long cnt = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            top.add(new SiteTrafficPathCountDto(String.valueOf(row[0]), cnt));
        }

        return SiteTrafficSummaryResponse.builder()
                .visitsLast15Minutes(c15)
                .visitsLast1Hour(c60)
                .visitsLast24Hours(c24)
                .topPathsLast24Hours(top)
                .build();
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            String first = comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String xReal = request.getHeader("X-Real-IP");
        if (xReal != null && !xReal.isBlank()) {
            return xReal.trim();
        }
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String safeSessionId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim();
        if (!s.matches("^[a-zA-Z0-9_-]{1,64}$")) {
            return "";
        }
        return s;
    }
}
