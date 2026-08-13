package com.mypropertyfact.estate.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypropertyfact.estate.dtos.IpTrackEventDto;
import com.mypropertyfact.estate.dtos.IpTrackEventPageResponse;
import com.mypropertyfact.estate.dtos.IpTrackHitRequest;
import com.mypropertyfact.estate.dtos.IpTrackIpPageResponse;
import com.mypropertyfact.estate.dtos.IpTrackIpSummaryDto;
import com.mypropertyfact.estate.dtos.IpTrackSummaryResponse;
import com.mypropertyfact.estate.entities.IpTrackEvent;
import com.mypropertyfact.estate.repositories.IpTrackEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpTrackService {

    public static final int PATH_MAX_LEN = 512;
    public static final int UA_MAX_LEN = 512;
    public static final int IP_MAX_LEN = 64;

    private static final long NORMAL_PATH_THROTTLE_MS = 60_000L;
    private static final Pattern SCAN_PATH = Pattern.compile(
            "(?i)(\\.env|\\.git|\\.aws|wp-admin|wp-login|phpmyadmin|xmlrpc|cgi-bin|"
                    + "actuator|/vendor/phpunit|/\\.svn|/\\.hg|\\.bak$|\\.sql($|\\?)|"
                    + "credentials|id_rsa|docker-compose|/adminer|/telescope|/debug|"
                    + "server-status|\\.DS_Store|web\\.config|phpinfo)",
            Pattern.CASE_INSENSITIVE);

    private final IpTrackEventRepository ipTrackEventRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Long> lastNormalHitByKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GeoCacheEntry> geoCache = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public static boolean isScanPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return SCAN_PATH.matcher(path).find();
    }

    public String validateAndNormalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String p = raw.trim();
        if (p.length() > PATH_MAX_LEN) {
            p = p.substring(0, PATH_MAX_LEN);
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (p.contains("\0") || p.contains("\\")) {
            return null;
        }
        return p;
    }

    @Transactional
    public void recordHit(IpTrackHitRequest body, HttpServletRequest request) {
        String path = validateAndNormalizePath(body != null ? body.getPath() : null);
        if (path == null) {
            return;
        }

        String ip = resolveClientIp(request);
        if (ip.isEmpty() && body != null && body.getClientIp() != null) {
            ip = truncate(body.getClientIp().trim(), IP_MAX_LEN);
        }
        if (ip.isEmpty()) {
            ip = "unknown";
        }

        boolean scan = isScanPath(path);
        Double gpsLat = body != null ? body.getLatitude() : null;
        Double gpsLon = body != null ? body.getLongitude() : null;
        boolean hasGps = gpsLat != null && gpsLon != null
                && gpsLat >= -90 && gpsLat <= 90
                && gpsLon >= -180 && gpsLon <= 180;

        if (!scan && !hasGps) {
            String key = ip + "|" + path;
            long now = System.currentTimeMillis();
            Long last = lastNormalHitByKey.get(key);
            if (last != null && now - last < NORMAL_PATH_THROTTLE_MS) {
                return;
            }
            // Also skip if DB already has a very recent same hit (multi-instance safety).
            LocalDateTime since = LocalDateTime.now().minusSeconds(55);
            if (ipTrackEventRepository.countByRemoteAddrAndPathAndOccurredAtAfter(ip, path, since) > 0) {
                lastNormalHitByKey.put(key, now);
                return;
            }
            lastNormalHitByKey.put(key, now);
        }

        String method = body != null && body.getMethod() != null
                ? truncate(body.getMethod().trim().toUpperCase(Locale.ROOT), 16)
                : truncate(safeHeader(request.getMethod()), 16);
        String ua = body != null && body.getUserAgent() != null && !body.getUserAgent().isBlank()
                ? truncate(body.getUserAgent(), UA_MAX_LEN)
                : truncate(safeHeader(request.getHeader("User-Agent")), UA_MAX_LEN);
        String source = body != null && body.getSource() != null && !body.getSource().isBlank()
                ? truncate(body.getSource().trim().toLowerCase(Locale.ROOT), 32)
                : "api";

        IpTrackEvent e = new IpTrackEvent();
        e.setRemoteAddr(truncate(ip, IP_MAX_LEN));
        e.setPath(path);
        e.setHttpMethod(method);
        e.setUserAgent(ua);
        e.setScan(scan);
        e.setSource(source);

        if (hasGps) {
            e.setLatitude(gpsLat);
            e.setLongitude(gpsLon);
        }

        GeoInfo geo = lookupGeo(ip);
        if (geo != null) {
            e.setCountry(truncate(geo.country, 64));
            e.setRegion(truncate(geo.region, 64));
            e.setCity(truncate(geo.city, 64));
            e.setOrg(truncate(geo.org, 255));
            if (!hasGps) {
                e.setLatitude(geo.latitude);
                e.setLongitude(geo.longitude);
            }
        }

        ipTrackEventRepository.save(e);
    }

    @Transactional(readOnly = true)
    public IpTrackSummaryResponse buildSummary() {
        LocalDateTime t24 = LocalDateTime.now().minusHours(24);
        return IpTrackSummaryResponse.builder()
                .totalHits(ipTrackEventRepository.count())
                .totalScans(ipTrackEventRepository.countByScanTrue())
                .uniqueIps(ipTrackEventRepository.countDistinctRemoteAddr())
                .uniqueScanIps(ipTrackEventRepository.countDistinctScanRemoteAddr())
                .hitsLast24Hours(ipTrackEventRepository.countByOccurredAtAfter(t24))
                .scansLast24Hours(ipTrackEventRepository.countByScanTrueAndOccurredAtAfter(t24))
                .build();
    }

    @Transactional(readOnly = true)
    public IpTrackIpPageResponse listIpSummaries(
            HttpServletRequest request, int page, int size, boolean scansOnly, Integer hours) {
        boolean reveal = SiteTrafficService.hasTrafficRevealCookie(request);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        LocalDateTime since = null;
        if (hours != null && hours > 0) {
            since = LocalDateTime.now().minusHours(Math.min(hours, 720));
        }

        Page<Object[]> raw = ipTrackEventRepository.aggregateByIp(
                scansOnly ? 1 : 0, since, PageRequest.of(safePage, safeSize));

        List<IpTrackIpSummaryDto> rows = new ArrayList<>();
        for (Object[] r : raw.getContent()) {
            if (r == null || r.length < 2 || r[0] == null) {
                continue;
            }
            String ip = String.valueOf(r[0]);
            long hitCount = toLong(r[1]);
            long scanCount = toLong(r[2]);
            LocalDateTime firstSeen = toLocalDateTime(r[3]);
            LocalDateTime lastSeen = toLocalDateTime(r[4]);
            String country = toStr(r.length > 5 ? r[5] : null);
            String region = toStr(r.length > 6 ? r[6] : null);
            String city = toStr(r.length > 7 ? r[7] : null);
            Double lat = toDouble(r.length > 8 ? r[8] : null);
            Double lon = toDouble(r.length > 9 ? r[9] : null);
            String org = toStr(r.length > 10 ? r[10] : null);

            // Prefer scan/probe paths (.env, .git, .aws, …) so they surface in admin UI.
            List<String> scanPaths = new ArrayList<>();
            List<String> visitPaths = new ArrayList<>();
            for (IpTrackEvent ev : ipTrackEventRepository.findTop20ByRemoteAddrOrderByOccurredAtDesc(ip)) {
                if (ev.getPath() == null) {
                    continue;
                }
                String label = ev.isScan() ? "SCAN " + ev.getPath() : ev.getPath();
                if (ev.isScan()) {
                    if (!scanPaths.contains(label) && scanPaths.size() < 8) {
                        scanPaths.add(label);
                    }
                } else if (!visitPaths.contains(label) && visitPaths.size() < 8) {
                    visitPaths.add(label);
                }
            }
            List<String> recentPaths = new ArrayList<>(scanPaths);
            for (String v : visitPaths) {
                if (recentPaths.size() >= 8) {
                    break;
                }
                if (!recentPaths.contains(v)) {
                    recentPaths.add(v);
                }
            }

            rows.add(IpTrackIpSummaryDto.builder()
                    .ip(reveal ? ip : maskIp(ip))
                    .hitCount(hitCount)
                    .scanCount(scanCount)
                    .firstSeen(firstSeen)
                    .lastSeen(lastSeen)
                    .country(country)
                    .region(region)
                    .city(city)
                    .latitude(lat)
                    .longitude(lon)
                    .org(org)
                    .locationLabel(formatLocation(city, region, country))
                    .recentPaths(recentPaths)
                    .ipRevealed(reveal)
                    .build());
        }

        return IpTrackIpPageResponse.builder()
                .content(rows)
                .totalElements(raw.getTotalElements())
                .totalPages(raw.getTotalPages())
                .number(raw.getNumber())
                .size(raw.getSize())
                .ipRevealActive(reveal)
                .build();
    }

    @Transactional(readOnly = true)
    public IpTrackEventPageResponse listEvents(
            HttpServletRequest request, String ipFilter, boolean scansOnly, int page, int size) {
        boolean reveal = SiteTrafficService.hasTrafficRevealCookie(request);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        Page<IpTrackEvent> p;
        if (ipFilter != null && !ipFilter.isBlank()) {
            p = ipTrackEventRepository.findByRemoteAddrOrderByOccurredAtDesc(
                    ipFilter.trim(), PageRequest.of(safePage, safeSize));
        } else if (scansOnly) {
            p = ipTrackEventRepository.findByScanTrueOrderByOccurredAtDesc(
                    PageRequest.of(safePage, safeSize));
        } else {
            p = ipTrackEventRepository.findByOrderByOccurredAtDesc(
                    PageRequest.of(safePage, safeSize));
        }

        List<IpTrackEventDto> rows = new ArrayList<>();
        for (IpTrackEvent e : p.getContent()) {
            String ip = e.getRemoteAddr();
            rows.add(IpTrackEventDto.builder()
                    .id(e.getId())
                    .occurredAt(e.getOccurredAt())
                    .ip(reveal ? (ip != null ? ip : "—") : maskIp(ip))
                    .path(e.getPath())
                    .httpMethod(e.getHttpMethod())
                    .userAgent(e.getUserAgent())
                    .country(e.getCountry())
                    .region(e.getRegion())
                    .city(e.getCity())
                    .latitude(e.getLatitude())
                    .longitude(e.getLongitude())
                    .org(e.getOrg())
                    .locationLabel(formatLocation(e.getCity(), e.getRegion(), e.getCountry()))
                    .scan(e.isScan())
                    .source(e.getSource())
                    .ipRevealed(reveal)
                    .build());
        }

        return IpTrackEventPageResponse.builder()
                .content(rows)
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .number(p.getNumber())
                .size(p.getSize())
                .ipRevealActive(reveal)
                .build();
    }

    private GeoInfo lookupGeo(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equals(ip) || isPrivateIp(ip)) {
            return null;
        }
        GeoCacheEntry cached = geoCache.get(ip);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.cachedAtMs < 24 * 60 * 60 * 1000L) {
            return cached.info;
        }
        try {
            String url = "http://ip-api.com/json/"
                    + java.net.URLEncoder.encode(ip, java.nio.charset.StandardCharsets.UTF_8)
                    + "?fields=status,country,regionName,city,lat,lon,org,query";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200 || res.body() == null || res.body().isBlank()) {
                return cached != null ? cached.info : null;
            }
            JsonNode node = objectMapper.readTree(res.body());
            if (!"success".equalsIgnoreCase(node.path("status").asText())) {
                return cached != null ? cached.info : null;
            }
            GeoInfo info = new GeoInfo(
                    blankToNull(node.path("country").asText(null)),
                    blankToNull(node.path("regionName").asText(null)),
                    blankToNull(node.path("city").asText(null)),
                    node.hasNonNull("lat") ? node.get("lat").asDouble() : null,
                    node.hasNonNull("lon") ? node.get("lon").asDouble() : null,
                    blankToNull(node.path("org").asText(null)));
            geoCache.put(ip, new GeoCacheEntry(info, now));
            return info;
        } catch (Exception ex) {
            log.debug("IP geo lookup failed for {}: {}", ip, ex.toString());
            return cached != null ? cached.info : null;
        }
    }

    private static boolean isPrivateIp(String ip) {
        return ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("127.")
                || ip.equals("::1")
                || ip.startsWith("fc")
                || ip.startsWith("fd")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.2")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.");
    }

    static String resolveClientIp(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) {
            return cf.trim();
        }
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

    private static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "—";
        }
        return "Unlock to view IP";
    }

    private static String formatLocation(String city, String region, String country) {
        List<String> parts = new ArrayList<>(3);
        if (city != null && !city.isBlank()) {
            parts.add(city);
        }
        if (region != null && !region.isBlank() && !region.equalsIgnoreCase(city)) {
            parts.add(region);
        }
        if (country != null && !country.isBlank()) {
            parts.add(country);
        }
        return parts.isEmpty() ? "—" : String.join(", ", parts);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String safeHeader(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        return s.trim();
    }

    private static String toStr(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o);
        if (s.isBlank() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }

    private static long toLong(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        if (o == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static Double toDouble(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime toLocalDateTime(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (o instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (o instanceof java.util.Date d) {
            return LocalDateTime.ofInstant(d.toInstant(), java.time.ZoneId.systemDefault());
        }
        try {
            return LocalDateTime.parse(String.valueOf(o).replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }

    private record GeoInfo(
            String country, String region, String city, Double latitude, Double longitude, String org) {}

    private record GeoCacheEntry(GeoInfo info, long cachedAtMs) {}
}
