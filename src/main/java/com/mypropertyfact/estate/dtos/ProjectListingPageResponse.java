package com.mypropertyfact.estate.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectListingPageResponse(
        String selectedDate,
        String selectedDateLabel,
        String relativeLabel,
        String zoneId,
        boolean rangeOverride,
        boolean truncated,
        ProjectListingSummaryDto summary,
        List<ProjectListingRowDto> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        LocalDateTime generatedAt
) {
}
