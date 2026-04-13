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
public class SiteTrafficVisitDto {
    private Long id;
    private LocalDateTime occurredAt;
    private String path;
    /** Milliseconds the visitor stayed on this path before the next navigation or tab close. */
    private Integer dwellMs;
    /** Client IP from X-Forwarded-For (or remote); masked until PIN unlock. */
    private String clientIp;
    private boolean ipRevealed;
}
