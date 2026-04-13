package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.WebStoryDto;
import com.mypropertyfact.estate.interfaces.WebStoryService;
import com.mypropertyfact.estate.models.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

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

    /**
     * Public AMP web story HTML. Sets charset and content-language so crawlers see
     * {@code lang} / {@code hreflang} in the document together with correct HTTP headers.
     */
    @GetMapping(value = "/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getWebStory(@PathVariable("slug") String slug) {
        String html = webStoryService.webStory(slug);
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_LANGUAGE, "en-IN")
                .body(html);
    }
}
