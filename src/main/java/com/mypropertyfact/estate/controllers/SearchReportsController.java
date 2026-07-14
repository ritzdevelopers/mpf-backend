package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.SearchReportResponse;
import com.mypropertyfact.estate.services.SearchAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/admin/super/search-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class SearchReportsController {

    private final SearchAnalyticsService searchAnalyticsService;

    /**
     * Aggregated weekly / monthly (or custom) search report for the admin dashboard.
     *
     * @param period week | month | custom
     */
    @GetMapping
    public ResponseEntity<SearchReportResponse> report(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(searchAnalyticsService.buildReport(period, from, to));
    }

    /** Multi-sheet Excel (.xlsx) export of the same report window. */
    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] bytes = searchAnalyticsService.buildExcelExport(period, from, to);
        String stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = "mpf-search-report-" + period + "-" + stamp + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
