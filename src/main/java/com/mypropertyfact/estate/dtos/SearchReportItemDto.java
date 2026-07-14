package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchReportItemDto {
    private String query;
    private String searchType;
    private long searchCount;
    private Long uniqueSessions;
    private String topTargetLabel;
}
