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
public class SiteTrafficDailyTrendResponse {
    private List<SiteTrafficDailyBucketDto> dailyBuckets;
    /** Page-view events in the last 7 calendar days (server timezone). */
    private long visitsLast7Days;
    /** The 7 calendar days immediately before that window. */
    private long visitsPrior7Days;
    /** Percent change of last 7 days vs the previous 7 days; null when the prior window had zero visits. */
    private Double percentChangeVsPrior7Days;
}
