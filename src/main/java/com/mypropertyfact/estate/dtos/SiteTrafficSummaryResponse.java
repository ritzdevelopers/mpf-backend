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
public class SiteTrafficSummaryResponse {
    private long visitsLast15Minutes;
    private long visitsLast1Hour;
    private long visitsLast24Hours;
    private List<SiteTrafficPathCountDto> topPathsLast24Hours;
}
