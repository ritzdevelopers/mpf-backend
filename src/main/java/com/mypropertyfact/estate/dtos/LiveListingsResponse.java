package com.mypropertyfact.estate.dtos;

import java.util.List;

public record LiveListingsResponse(
        LiveListingSummaryDto summary,
        List<LiveListingRowDto> rows
) {
}
