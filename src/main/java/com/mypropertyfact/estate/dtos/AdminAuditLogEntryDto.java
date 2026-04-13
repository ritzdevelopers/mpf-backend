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
public class AdminAuditLogEntryDto {
    private Long id;
    private LocalDateTime occurredAt;
    private String actorEmail;
    private Integer actorUserId;
    private String httpMethod;
    private String requestPath;
    private String queryString;
    private int httpStatus;
    private boolean success;
    private int durationMs;
    private String taskLabel;
    private String clientAdminPage;
    private Integer clientDwellMs;
}
