package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpTrackSummaryResponse {
    private long totalHits;
    private long totalScans;
    private long uniqueIps;
    private long uniqueScanIps;
    private long hitsLast24Hours;
    private long scansLast24Hours;
}
