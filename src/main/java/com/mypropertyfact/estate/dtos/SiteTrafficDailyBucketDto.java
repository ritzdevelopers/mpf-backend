package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteTrafficDailyBucketDto {
    /** ISO calendar date in server timezone, e.g. 2026-04-13 */
    private String date;
    private long count;
}
