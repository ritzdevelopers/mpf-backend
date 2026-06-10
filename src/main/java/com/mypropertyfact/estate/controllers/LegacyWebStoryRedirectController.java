package com.mypropertyfact.estate.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Legacy URLs on the API host used {@code /web-story/{slug}} before {@code /api/v1/} was introduced.
 * Redirect permanently to the canonical AMP story path or image path.
 */
@RestController
public class LegacyWebStoryRedirectController {

    private static final Pattern IMAGE_EXTENSION =
            Pattern.compile(".*\\.(jpg|jpeg|png|webp|gif)$", Pattern.CASE_INSENSITIVE);

    @GetMapping("/web-story/{segment}")
    public ResponseEntity<Void> redirectLegacyWebStory(@PathVariable String segment) {
        String destination = IMAGE_EXTENSION.matcher(segment).matches()
                ? "/api/v1/get/images/web-story/" + segment
                : "/api/v1/web-story/" + segment;
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .location(URI.create(destination))
                .build();
    }
}
