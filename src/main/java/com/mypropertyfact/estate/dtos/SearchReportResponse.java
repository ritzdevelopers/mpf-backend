package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchReportResponse {
    /** week | month | custom */
    private String period;
    private String fromInclusive;
    private String toExclusive;
    private long totalSearches;
    private long propertySearches;
    private long blogSearches;
    private long keywordSearches;
    private long uniqueQueries;
    private List<SearchReportItemDto> topKeywords;
    private List<SearchReportItemDto> topPropertySearches;
    private List<SearchReportItemDto> topBlogSearches;
    private List<SearchReportItemDto> topOverall;
    private List<SearchReportDailyBucketDto> dailyTrend;
}
