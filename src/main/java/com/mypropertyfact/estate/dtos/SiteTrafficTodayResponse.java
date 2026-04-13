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
public class SiteTrafficTodayResponse {
    /** One bucket per clock hour (0–23) for the calendar day; counts reset the next day. */
    private List<SiteTrafficHourBucketDto> hourlyBuckets;
    /** Calendar date of "today" in server zone (yyyy-MM-dd). */
    private String calendarDate;
    /** IANA zone id used for the day boundary, e.g. Asia/Kolkata */
    private String zoneId;
    /** Views from midnight today through now. */
    private long todayTotalSoFar;
    /** Full previous calendar day (midnight–midnight). */
    private long yesterdayFullDayTotal;
    /** Same elapsed window yesterday (midnight yesterday → same time as now). */
    private long yesterdaySameWindowTotal;
    /**
     * (todaySoFar − yesterdaySameWindow) / yesterdaySameWindow × 100;
     * null when yesterday same-window was 0.
     */
    private Double percentChangeVsYesterdaySameWindow;
    /**
     * todaySoFar / yesterdayFullDay × 100 — "so far today we are at X% of all of yesterday".
     * null when yesterday full day was 0.
     */
    private Double todaySoFarPercentOfYesterdayFullDay;
    private String generatedAt;
}
