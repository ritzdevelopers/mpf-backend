package com.mypropertyfact.estate.dtos;

import java.time.LocalDateTime;

public record ProjectListingRowDto(
        String source,
        String listingTypeLabel,
        Long id,
        String displayId,
        String projectName,
        String listedBy,
        String userEmail,
        String userRole,
        Integer userId,
        String listedDate,
        String listedTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String location,
        String developer,
        String status,
        String statusKey,
        String adminPath,
        String publicPath
) {
}
