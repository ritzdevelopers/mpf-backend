package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpTrackIpSummaryDto {
    private String ip;
    private long hitCount;
    private long scanCount;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private String country;
    private String region;
    private String city;
    private Double latitude;
    private Double longitude;
    private String org;
    private String locationLabel;
    /** Most recent paths this IP hit (newest first). */
    private List<String> recentPaths;
    private boolean ipRevealed;
}
