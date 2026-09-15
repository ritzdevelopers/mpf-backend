package com.mypropertyfact.estate.dtos;

import java.util.List;

public record ProjectListingFilterOptionsDto(
        List<String> developers,
        List<String> locations,
        List<String> statuses,
        List<String> userRoles
) {
}
