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
public class AdminManagementActivityItemDto {
    private LocalDateTime occurredAt;
    /** Human-readable line kept for older clients. */
    private String event;
    /** Who performed the action. */
    private String actorName;
    private String actorEmail;
    private Integer actorUserId;
    /** What they did (short task label). */
    private String action;
    /** GET / POST / PUT / DELETE — used for visual action type. */
    private String httpMethod;
    private boolean success;
    private String requestPath;
}
