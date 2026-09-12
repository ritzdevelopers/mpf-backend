package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.ListingPageContentDto;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.ListingPageContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/listing-page-contents")
@RequiredArgsConstructor
public class ListingPageContentController {

    private final ListingPageContentService listingPageContentService;

    @GetMapping("/get-all")
    public ResponseEntity<List<Map<String, Object>>> getAllContents() {
        return new ResponseEntity<>(listingPageContentService.getAllContents(), HttpStatus.OK);
    }

    @GetMapping("/get-by-slug")
    public ResponseEntity<Map<String, Object>> getBySlugParam(@RequestParam("slug") String slug) {
        return new ResponseEntity<>(listingPageContentService.getBySlug(slug), HttpStatus.OK);
    }

    @GetMapping("/get/{slug}")
    public ResponseEntity<Map<String, Object>> getBySlug(@PathVariable("slug") String slug) {
        return new ResponseEntity<>(listingPageContentService.getBySlug(slug), HttpStatus.OK);
    }

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_LISTING_FAQS')")
    public ResponseEntity<Response> addUpdateContent(@RequestBody ListingPageContentDto dto) {
        return new ResponseEntity<>(listingPageContentService.addUpdateContent(dto), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_LISTING_FAQS')")
    public ResponseEntity<Response> deleteContent(@PathVariable("id") int id) {
        return new ResponseEntity<>(listingPageContentService.deleteContent(id), HttpStatus.OK);
    }
}
