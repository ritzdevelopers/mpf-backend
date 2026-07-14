package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchReportDailyBucketDto {
    private String date;
    private long total;
    private long property;
    private long blog;
    private long keyword;
}
