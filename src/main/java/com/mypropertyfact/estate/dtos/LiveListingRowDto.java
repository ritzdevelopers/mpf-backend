package com.mypropertyfact.estate.dtos;

import java.time.LocalDateTime;

public record LiveListingRowDto(
        String source,
        String sourceLabel,
        String listerType,
        Long id,
        String title,
        String projectName,
        String builderName,
        String city,
        String locality,
        String listingType,
        String transaction,
        String configuration,
        String price,
        String statusLabel,
        String listedBy,
        String listedByEmail,
        LocalDateTime createdAt,
        LocalDateTime wentLiveAt,
        String publicPath,
        String adminPath
) {
}
