package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.SiteTrafficPingRequest;
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

    @PostMapping("/site-traffic")
    public ResponseEntity<?> ping(@RequestBody SiteTrafficPingRequest body, HttpServletRequest request) {
        String path = siteTrafficService.validateAndNormalizePath(body != null ? body.getPath() : null);
        if (path == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_path"));
        }
        String sessionId = body != null ? body.getClientSessionId() : null;
        Long dwellMs = body != null ? body.getDwellMs() : null;
        siteTrafficService.recordVisit(path, dwellMs, sessionId, request);
        return ResponseEntity.accepted().build();
    }
}
