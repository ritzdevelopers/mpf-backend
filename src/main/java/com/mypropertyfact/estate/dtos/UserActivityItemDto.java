package com.mypropertyfact.estate.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class UserActivityItemDto {
    Long id;
    String type;
    String entityType;
    String entityId;
    String entitySlug;
    String entityLabel;
    String href;
    LocalDateTime at;
}
