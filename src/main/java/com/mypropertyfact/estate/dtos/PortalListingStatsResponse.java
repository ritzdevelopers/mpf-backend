package com.mypropertyfact.estate.dtos;

import java.util.List;

public record PortalListingStatsResponse(
        PortalListingStatsSummaryDto summary,
        List<PortalListingStatsRowDto> rows
) {
}
