package com.mypropertyfact.estate.dtos;

import java.time.LocalDateTime;

public record ProjectListingActivityDto(
        String action,
        String actionLabel,
        String actorName,
        Integer actorUserId,
        String date,
        String time,
        LocalDateTime occurredAt,
        String detail
) {
}
