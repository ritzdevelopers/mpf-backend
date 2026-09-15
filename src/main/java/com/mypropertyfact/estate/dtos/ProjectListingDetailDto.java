package com.mypropertyfact.estate.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectListingDetailDto(
        String source,
        String listingTypeLabel,
        Long id,
        String displayId,
        String projectName,
        String developer,
        String location,
        String status,
        String statusKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdDate,
        String createdTime,
        String updatedDate,
        String updatedTime,
        Integer userId,
        String userName,
        String userEmail,
        String userPhone,
        String userRole,
        String avatar,
        String adminPath,
        String publicPath,
        List<ProjectListingActivityDto> activity
) {
}
