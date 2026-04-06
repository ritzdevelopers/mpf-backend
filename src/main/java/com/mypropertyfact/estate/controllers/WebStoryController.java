package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.WebStoryDto;
import com.mypropertyfact.estate.interfaces.WebStoryService;
import com.mypropertyfact.estate.models.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/web-story")
@RequiredArgsConstructor
public class WebStoryController {

    private final WebStoryService webStoryService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllWebStories() {
        return ResponseEntity.ok(webStoryService.getAllWebStories());
    }

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_WEB_STORIES')")
    public ResponseEntity<?> addUpdate(@RequestParam(required = false) MultipartFile image,
                                       @ModelAttribute WebStoryDto webStoryDto) {
        return ResponseEntity.ok(webStoryService.addUpdateWebStory(image, webStoryDto));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_WEB_STORIES')")
    public ResponseEntity<Response> deleteWebStory(@PathVariable("id") int id) {
        return ResponseEntity.ok(webStoryService.deleteWebStory(id));
    }

    @GetMapping(value = "/{slug}", produces = "text/html")
    public String getWebStory(@PathVariable("slug") String slug) {
        return webStoryService.webStory(slug);
    }
}
