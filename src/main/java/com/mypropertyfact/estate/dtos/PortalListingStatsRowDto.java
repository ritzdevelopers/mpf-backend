package com.mypropertyfact.estate.dtos;

public record PortalListingStatsRowDto(
        Integer userId,
        String fullName,
        String email,
        String phone,
        String userType,
        String userCategory,
        boolean enabled,
        boolean verified,
        long totalListings,
        long liveListings,
        long pendingListings,
        long draftListings,
        long rejectedListings
) {
}
