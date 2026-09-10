package com.mypropertyfact.estate.dtos;

public record PortalListingStatsSummaryDto(
        long totalPortalUsers,
        long brokers,
        long owners,
        long totalListings,
        long liveListings,
        long pendingListings,
        long draftListings,
        long rejectedListings
) {
}
