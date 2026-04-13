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
public class SiteTrafficLiveSeriesResponse {
    private List<SiteTrafficLiveBucketDto> buckets;
    private int windowMinutes;
    /** Rolling counts aligned with super-tracking summary windows */
    private long visitsLast15Minutes;
    private long visitsLast1Hour;
    private String generatedAt;
}
