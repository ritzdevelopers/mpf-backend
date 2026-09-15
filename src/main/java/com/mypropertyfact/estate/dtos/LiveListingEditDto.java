package com.mypropertyfact.estate.dtos;

import java.time.LocalDateTime;

public record LiveListingEditDto(
        String actorName,
        String action,
        String actionLabel,
        String detail,
        LocalDateTime occurredAt
) {
}
