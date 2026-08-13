package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.IpTrackHitRequest;
import com.mypropertyfact.estate.dtos.SiteTrafficPingRequest;
import com.mypropertyfact.estate.services.IpTrackService;
import com.mypropertyfact.estate.services.SiteTrafficService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicTrafficController {

    private final SiteTrafficService siteTrafficService;
    private final IpTrackService ipTrackService;

    @PostMapping("/site-traffic")
    public ResponseEntity<?> ping(@RequestBody SiteTrafficPingRequest body, HttpServletRequest request) {
        String path = siteTrafficService.validateAndNormalizePath(body != null ? body.getPath() : null);
        if (path == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_path"));
        }
        String sessionId = body != null ? body.getClientSessionId() : null;
        Long dwellMs = body != null ? body.getDwellMs() : null;
        siteTrafficService.recordVisit(path, dwellMs, sessionId, request);

        // Also feed the IP tracker (throttled for normal paths; scans always recorded).
        if (dwellMs == null) {
            IpTrackHitRequest hit = new IpTrackHitRequest();
            hit.setPath(path);
            hit.setMethod("GET");
            hit.setSource("beacon");
            hit.setUserAgent(request.getHeader("User-Agent"));
            if (body != null) {
                hit.setLatitude(body.getLatitude());
                hit.setLongitude(body.getLongitude());
            }
            ipTrackService.recordHit(hit, request);
        }
        return ResponseEntity.accepted().build();
    }
}
