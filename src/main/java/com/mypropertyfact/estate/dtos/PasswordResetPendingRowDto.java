package com.mypropertyfact.estate.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PasswordResetPendingRowDto {
    Long id;
    Integer userId;
    String email;
    String fullName;
    String dashboardUsername;
    LocalDateTime requestedAt;
}
