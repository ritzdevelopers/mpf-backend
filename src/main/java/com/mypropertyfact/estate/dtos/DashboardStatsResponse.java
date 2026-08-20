package com.mypropertyfact.estate.dtos;

public record DashboardStatsResponse(
    long userCount,
    long enquiryCount,
    long projectCount,
    long blogCount,
    long blogCategoryCount,
    long cityCount,
    long builderCount,
    long amenityCount,
    long projectTypeCount
) {
}
