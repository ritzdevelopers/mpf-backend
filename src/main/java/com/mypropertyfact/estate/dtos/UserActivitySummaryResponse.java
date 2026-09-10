package com.mypropertyfact.estate.dtos;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserActivitySummaryResponse {
    long viewedCount;
    long shortlistedCount;
    long searchCount;
    List<UserActivityItemDto> recent;
}
