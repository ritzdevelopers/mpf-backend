package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.IpTrackHitRequest;
import com.mypropertyfact.estate.services.IpTrackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicIpTrackController {

    private final IpTrackService ipTrackService;

    @PostMapping("/ip-track")
    public ResponseEntity<Void> record(
            @RequestBody(required = false) IpTrackHitRequest body, HttpServletRequest request) {
        ipTrackService.recordHit(body, request);
        return ResponseEntity.accepted().build();
    }
}
