package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminNotificationItemDto {
    /** Stable id for client de-duplication, e.g. "audit:42" or "enquiry:15". */
    private String id;
    /** BLOG | ENQUIRY | PROPERTY | PERMISSION */
    private String type;
    private String title;
    private String message;
    private String actorName;
    private String taskLabel;
    private LocalDateTime occurredAt;
    private String href;
    private boolean unread;
}
