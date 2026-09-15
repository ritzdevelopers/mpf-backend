package com.mypropertyfact.estate.dtos;

public record ProjectListingSummaryDto(
        long totalListings,
        long normalProjects,
        long portalBrokerProjects,
        long active,
        long pending,
        long rejected
) {
}
