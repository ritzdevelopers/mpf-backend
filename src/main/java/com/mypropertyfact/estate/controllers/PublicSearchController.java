package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.SearchEventRequest;
import com.mypropertyfact.estate.services.SearchAnalyticsService;
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
public class PublicSearchController {

    private final SearchAnalyticsService searchAnalyticsService;

    @PostMapping("/search-event")
    public ResponseEntity<?> record(@RequestBody SearchEventRequest body, HttpServletRequest request) {
        searchAnalyticsService.recordSearch(body, request);
        return ResponseEntity.accepted().build();
    }
}
