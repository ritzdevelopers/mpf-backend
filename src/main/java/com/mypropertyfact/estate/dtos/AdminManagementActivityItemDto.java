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
    /** Human-readable line, e.g. "User Jane Doe (jane@x.com) — Create or update blog post". */
    private String event;
}
