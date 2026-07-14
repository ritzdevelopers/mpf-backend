package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.SearchEventRequest;
import com.mypropertyfact.estate.dtos.SearchReportDailyBucketDto;
import com.mypropertyfact.estate.dtos.SearchReportItemDto;
import com.mypropertyfact.estate.dtos.SearchReportResponse;
import com.mypropertyfact.estate.entities.SearchQueryEvent;
import com.mypropertyfact.estate.repositories.SearchQueryEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SearchAnalyticsService {

    private static final int QUERY_MAX = 512;
    private static final int PATH_MAX = 512;
    private static final int REF_MAX = 255;
    private static final int SESSION_MAX = 64;
    private static final int IP_MAX = 64;
    private static final int DEDUPE_SECONDS = 8;
    private static final int TOP_LIMIT = 50;

    private final SearchQueryEventRepository searchQueryEventRepository;
    private final ConcurrentHashMap<String, Long> lastRecordByKey = new ConcurrentHashMap<>();

    @Transactional
    public void recordSearch(SearchEventRequest body, HttpServletRequest request) {
        if (body == null) {
            return;
        }
        String query = sanitizeQuery(body.getQuery());
        if (query.isEmpty()) {
            return;
        }
        String type = normalizeType(body.getSearchType());
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        String session = truncate(safeSessionId(body.getClientSessionId()), SESSION_MAX);
        String ip = truncate(resolveClientIp(request), IP_MAX);

        String throttleKey = (session.isEmpty() ? ip : session) + "|" + type + "|" + normalized;
        long now = System.currentTimeMillis();
        Long last = lastRecordByKey.get(throttleKey);
        if (last != null && now - last < DEDUPE_SECONDS * 1000L) {
            return;
        }
        if (!session.isEmpty()) {
            LocalDateTime since = LocalDateTime.now().minusSeconds(DEDUPE_SECONDS);
            if (searchQueryEventRepository.countRecentDuplicate(session, normalized, type, since) > 0) {
                lastRecordByKey.put(throttleKey, now);
                return;
            }
        }
        lastRecordByKey.put(throttleKey, now);

        SearchQueryEvent e = new SearchQueryEvent();
        e.setQueryText(query);
        e.setQueryNormalized(normalized);
        e.setSearchType(type);
        e.setTargetRef(truncate(nullToEmpty(body.getTargetRef()), REF_MAX));
        e.setTargetLabel(truncate(nullToEmpty(body.getTargetLabel()), REF_MAX));
        if (e.getTargetRef().isEmpty()) {
            e.setTargetRef(null);
        }
        if (e.getTargetLabel().isEmpty()) {
            e.setTargetLabel(null);
        }
        e.setResultCount(body.getResultCount());
        String path = sanitizePath(body.getSourcePath());
        e.setSourcePath(path.isEmpty() ? null : path);
        e.setClientSessionId(session.isEmpty() ? null : session);
        e.setRemoteAddr(ip.isEmpty() ? null : ip);
        searchQueryEventRepository.save(e);
    }

    public SearchReportResponse buildReport(String period, LocalDate fromDate, LocalDate toDate) {
        LocalDateTimeRange range = resolveRange(period, fromDate, toDate);
        LocalDateTime from = range.from;
        LocalDateTime to = range.to;

        long total = searchQueryEventRepository.countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(from, to);
        long property = searchQueryEventRepository.countBySearchTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                SearchQueryEvent.TYPE_PROPERTY, from, to);
        long blog = searchQueryEventRepository.countBySearchTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                SearchQueryEvent.TYPE_BLOG, from, to);
        long keyword = searchQueryEventRepository.countBySearchTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                SearchQueryEvent.TYPE_KEYWORD, from, to);
        long unique = searchQueryEventRepository.countDistinctQueriesInRange(from, to);

        PageRequest topPage = PageRequest.of(0, TOP_LIMIT);
        List<SearchReportItemDto> topOverall = mapGrouped(searchQueryEventRepository.topQueriesGrouped(from, to, topPage));
        List<SearchReportItemDto> topProperty = mapGrouped(
                searchQueryEventRepository.topQueriesByType(from, to, SearchQueryEvent.TYPE_PROPERTY, topPage));
        List<SearchReportItemDto> topBlog = mapGrouped(
                searchQueryEventRepository.topQueriesByType(from, to, SearchQueryEvent.TYPE_BLOG, topPage));
        List<SearchReportItemDto> topKeywords = mapGrouped(
                searchQueryEventRepository.topQueriesByType(from, to, SearchQueryEvent.TYPE_KEYWORD, topPage));

        List<SearchReportDailyBucketDto> daily = mapDaily(searchQueryEventRepository.dailyBuckets(from, to));

        return SearchReportResponse.builder()
                .period(range.periodLabel)
                .fromInclusive(from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .toExclusive(to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .totalSearches(total)
                .propertySearches(property)
                .blogSearches(blog)
                .keywordSearches(keyword)
                .uniqueQueries(unique)
                .topKeywords(topKeywords)
                .topPropertySearches(topProperty)
                .topBlogSearches(topBlog)
                .topOverall(topOverall)
                .dailyTrend(daily)
                .build();
    }

    public byte[] buildExcelExport(String period, LocalDate fromDate, LocalDate toDate) {
        SearchReportResponse report = buildReport(period, fromDate, toDate);
        LocalDateTimeRange range = resolveRange(period, fromDate, toDate);
        List<SearchQueryEvent> raw =
                searchQueryEventRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                        range.from, range.to);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSummarySheet(workbook, report);
            writeRankedSheet(workbook, "Top Overall", report.getTopOverall());
            writeRankedSheet(workbook, "Property Searches", report.getTopPropertySearches());
            writeRankedSheet(workbook, "Blog Searches", report.getTopBlogSearches());
            writeRankedSheet(workbook, "Keywords", report.getTopKeywords());
            writeDailySheet(workbook, report.getDailyTrend());
            writeRawSheet(workbook, raw);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build search report Excel", ex);
        }
    }

    private void writeSummarySheet(Workbook workbook, SearchReportResponse report) {
        Sheet sheet = workbook.createSheet("Summary");
        String[][] rows = {
                {"Period", report.getPeriod()},
                {"From", report.getFromInclusive()},
                {"To", report.getToExclusive()},
                {"Total Searches", String.valueOf(report.getTotalSearches())},
                {"Property Searches", String.valueOf(report.getPropertySearches())},
                {"Blog Searches", String.valueOf(report.getBlogSearches())},
                {"Keyword Searches", String.valueOf(report.getKeywordSearches())},
                {"Unique Queries", String.valueOf(report.getUniqueQueries())},
        };
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(rows[i][0]);
            row.createCell(1).setCellValue(rows[i][1]);
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writeRankedSheet(Workbook workbook, String name, List<SearchReportItemDto> items) {
        Sheet sheet = workbook.createSheet(truncateSheetName(name));
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Rank");
        header.createCell(1).setCellValue("Query");
        header.createCell(2).setCellValue("Type");
        header.createCell(3).setCellValue("Search Count");
        header.createCell(4).setCellValue("Unique Sessions");
        header.createCell(5).setCellValue("Top Target");
        int r = 1;
        if (items != null) {
            for (SearchReportItemDto item : items) {
                Row row = sheet.createRow(r);
                row.createCell(0).setCellValue(r);
                row.createCell(1).setCellValue(nullSafe(item.getQuery()));
                row.createCell(2).setCellValue(nullSafe(item.getSearchType()));
                row.createCell(3).setCellValue(item.getSearchCount());
                row.createCell(4).setCellValue(item.getUniqueSessions() == null ? 0 : item.getUniqueSessions());
                row.createCell(5).setCellValue(nullSafe(item.getTopTargetLabel()));
                r++;
            }
        }
        for (int c = 0; c < 6; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    private void writeDailySheet(Workbook workbook, List<SearchReportDailyBucketDto> daily) {
        Sheet sheet = workbook.createSheet("Daily Trend");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("Total");
        header.createCell(2).setCellValue("Property");
        header.createCell(3).setCellValue("Blog");
        header.createCell(4).setCellValue("Keyword");
        int r = 1;
        if (daily != null) {
            for (SearchReportDailyBucketDto b : daily) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nullSafe(b.getDate()));
                row.createCell(1).setCellValue(b.getTotal());
                row.createCell(2).setCellValue(b.getProperty());
                row.createCell(3).setCellValue(b.getBlog());
                row.createCell(4).setCellValue(b.getKeyword());
            }
        }
        for (int c = 0; c < 5; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    private void writeRawSheet(Workbook workbook, List<SearchQueryEvent> events) {
        Sheet sheet = workbook.createSheet("Raw Events");
        Row header = sheet.createRow(0);
        String[] cols = {
                "Occurred At", "Query", "Type", "Target Label", "Target Ref", "Results", "Source Path", "Session"
        };
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }
        int r = 1;
        int limit = Math.min(events.size(), 5000);
        for (int i = 0; i < limit; i++) {
            SearchQueryEvent e = events.get(i);
            Row row = sheet.createRow(r++);
            row.createCell(0)
                    .setCellValue(
                            e.getOccurredAt() == null
                                    ? ""
                                    : e.getOccurredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            row.createCell(1).setCellValue(nullSafe(e.getQueryText()));
            row.createCell(2).setCellValue(nullSafe(e.getSearchType()));
            row.createCell(3).setCellValue(nullSafe(e.getTargetLabel()));
            row.createCell(4).setCellValue(nullSafe(e.getTargetRef()));
            Cell results = row.createCell(5);
            if (e.getResultCount() != null) {
                results.setCellValue(e.getResultCount());
            }
            row.createCell(6).setCellValue(nullSafe(e.getSourcePath()));
            row.createCell(7).setCellValue(nullSafe(e.getClientSessionId()));
        }
        for (int c = 0; c < cols.length; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    private static List<SearchReportItemDto> mapGrouped(List<Object[]> rows) {
        List<SearchReportItemDto> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Object[] row : rows) {
            String query = row[0] == null ? "" : String.valueOf(row[0]);
            String type = row[1] == null ? "" : String.valueOf(row[1]);
            long count = row[2] instanceof Number n ? n.longValue() : 0L;
            Long sessions = row[3] instanceof Number n ? n.longValue() : 0L;
            String label = row[4] == null ? null : String.valueOf(row[4]);
            out.add(SearchReportItemDto.builder()
                    .query(query)
                    .searchType(type)
                    .searchCount(count)
                    .uniqueSessions(sessions)
                    .topTargetLabel(label)
                    .build());
        }
        return out;
    }

    private static List<SearchReportDailyBucketDto> mapDaily(List<Object[]> rows) {
        List<SearchReportDailyBucketDto> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Object[] row : rows) {
            String date;
            if (row[0] instanceof java.sql.Date d) {
                date = d.toLocalDate().toString();
            } else if (row[0] instanceof LocalDate ld) {
                date = ld.toString();
            } else {
                date = row[0] == null ? "" : String.valueOf(row[0]);
            }
            out.add(SearchReportDailyBucketDto.builder()
                    .date(date)
                    .total(asLong(row[1]))
                    .property(asLong(row[2]))
                    .blog(asLong(row[3]))
                    .keyword(asLong(row[4]))
                    .build());
        }
        return out;
    }

    private static long asLong(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private LocalDateTimeRange resolveRange(String period, LocalDate fromDate, LocalDate toDate) {
        String p = period == null ? "week" : period.trim().toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        if ("custom".equals(p) && fromDate != null && toDate != null) {
            LocalDateTime from = fromDate.atStartOfDay();
            LocalDateTime to = toDate.plusDays(1).atStartOfDay();
            if (!to.isAfter(from)) {
                to = from.plusDays(1);
            }
            return new LocalDateTimeRange("custom", from, to);
        }
        if ("month".equals(p)) {
            LocalDateTime from = now.minusDays(30);
            return new LocalDateTimeRange("month", from, now.plusSeconds(1));
        }
        // default week = last 7 days
        LocalDateTime from = now.minusDays(7);
        return new LocalDateTimeRange("week", from, now.plusSeconds(1));
    }

    private static String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return SearchQueryEvent.TYPE_KEYWORD;
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case SearchQueryEvent.TYPE_PROPERTY, "project", "projects" -> SearchQueryEvent.TYPE_PROPERTY;
            case SearchQueryEvent.TYPE_BLOG, "blogs" -> SearchQueryEvent.TYPE_BLOG;
            default -> SearchQueryEvent.TYPE_KEYWORD;
        };
    }

    private static String sanitizeQuery(String raw) {
        if (raw == null) {
            return "";
        }
        String q = raw.trim().replaceAll("\\s+", " ");
        if (q.length() > QUERY_MAX) {
            q = q.substring(0, QUERY_MAX);
        }
        if (q.length() < 2) {
            return "";
        }
        return q;
    }

    private static String sanitizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String p = raw.trim();
        if (p.length() > PATH_MAX) {
            p = p.substring(0, PATH_MAX);
        }
        if (!p.startsWith("/") || p.contains("..") || p.contains("\\") || p.contains("\0")) {
            return "";
        }
        int q = p.indexOf('?');
        if (q >= 0) {
            p = p.substring(0, q);
        }
        return p;
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

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String truncateSheetName(String name) {
        if (name == null) {
            return "Sheet";
        }
        return name.length() <= 31 ? name : name.substring(0, 31);
    }

    private record LocalDateTimeRange(String periodLabel, LocalDateTime from, LocalDateTime to) {}
}
