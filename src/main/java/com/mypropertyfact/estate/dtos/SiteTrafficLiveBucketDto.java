package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteTrafficLiveBucketDto {
    /** ISO-8601 local minute start, e.g. 2026-04-13T14:05:00 */
    private String minuteStart;
    /** Short clock label for the chart axis, e.g. 14:05 */
    private String label;
    private long count;
}
