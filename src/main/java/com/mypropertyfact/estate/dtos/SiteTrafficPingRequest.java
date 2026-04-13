package com.mypropertyfact.estate.dtos;

import lombok.Data;

@Data
public class SiteTrafficPingRequest {
    /** Normalized site path, e.g. /projects/foo */
    private String path;
    /** Optional opaque id from the browser (e.g. random session key in sessionStorage). */
    private String clientSessionId;

    /**
     * Milliseconds spent on {@code path} before navigating away or closing the tab.
     * When set, this row represents a completed page visit (not a simple ping).
     */
    private Long dwellMs;
}
