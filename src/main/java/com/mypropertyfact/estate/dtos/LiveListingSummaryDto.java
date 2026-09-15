package com.mypropertyfact.estate.dtos;

public record LiveListingSummaryDto(
        long totalLive,
        long websiteProjects,
        long portalListings,
        long brokerListings,
        long ownerListings
) {
}
