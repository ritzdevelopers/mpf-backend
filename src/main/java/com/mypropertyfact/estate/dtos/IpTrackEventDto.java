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
public class IpTrackEventDto {
    private Long id;
    private LocalDateTime occurredAt;
    private String ip;
    private String path;
    private String httpMethod;
    private String userAgent;
    private String country;
    private String region;
    private String city;
    private Double latitude;
    private Double longitude;
    private String org;
    private String locationLabel;
    private boolean scan;
    private String source;
    private boolean ipRevealed;
}
