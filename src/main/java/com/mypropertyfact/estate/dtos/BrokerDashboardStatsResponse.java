package com.mypropertyfact.estate.dtos;

public record BrokerDashboardStatsResponse(
    long totalListings,
    long liveListings,
    long pendingListings,
    long draftListings,
    long rejectedListings,
    long enquiryCount,
    long addedThisMonth,
    long cityCount,
    long builderCount,
    long amenityCount,
    long propertyTypeCount
) {
}
