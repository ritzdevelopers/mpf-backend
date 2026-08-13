package com.mypropertyfact.estate.dtos;

import lombok.Data;

@Data
public class IpTrackHitRequest {
    private String path;
    private String method;
    private String userAgent;
    private String source;
    /** Optional client-reported IP (usually ignored; server uses request headers). */
    private String clientIp;
    /** Optional GPS from the device (real visitors who allow location). */
    private Double latitude;
    private Double longitude;
}
