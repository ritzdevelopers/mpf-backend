package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteTrafficHourBucketDto {
    /** 0–23 in the reporting zone */
    private int hour;
    /** Clock label, e.g. 06:00 */
    private String label;
    private long count;
}
