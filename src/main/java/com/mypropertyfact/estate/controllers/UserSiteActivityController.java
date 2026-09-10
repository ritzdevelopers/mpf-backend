package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.UserActivitySummaryResponse;
import com.mypropertyfact.estate.dtos.UserActivityUpsertRequest;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.services.UserSiteActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/activity")
@RequiredArgsConstructor
public class UserSiteActivityController {

    private final UserSiteActivityService service;

    @GetMapping
    public ResponseEntity<UserActivitySummaryResponse> summary() {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.summary(user.getId()));
    }

    @PostMapping
    public ResponseEntity<UserActivitySummaryResponse> upsert(@RequestBody UserActivityUpsertRequest request) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.upsert(user.getId(), request != null ? request : new UserActivityUpsertRequest()));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }
}
