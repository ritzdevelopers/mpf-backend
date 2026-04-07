package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardActivityItemDto {
    private String taskType;
    private String title;
    private String href;
    private LocalDateTime occurredAt;
}
