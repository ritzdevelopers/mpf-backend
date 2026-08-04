package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.ListingPageFaqDto;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.ListingPageFaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/listing-page-faqs")
@RequiredArgsConstructor
public class ListingPageFaqController {

    private final ListingPageFaqService listingPageFaqService;

    @GetMapping("/get-all")
    public ResponseEntity<List<Map<String, Object>>> getAllFaqs() {
        return new ResponseEntity<>(listingPageFaqService.getAllFaqsGrouped(), HttpStatus.OK);
    }

    @GetMapping("/get/{slug}")
    public ResponseEntity<List<Map<String, Object>>> getBySlug(@PathVariable("slug") String slug) {
        return new ResponseEntity<>(listingPageFaqService.getBySlug(slug), HttpStatus.OK);
    }

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_LISTING_FAQS')")
    public ResponseEntity<Response> addUpdateFaq(@RequestBody ListingPageFaqDto dto) {
        return new ResponseEntity<>(listingPageFaqService.addUpdateFaq(dto), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_LISTING_FAQS')")
    public ResponseEntity<Response> deleteFaq(@PathVariable("id") int id) {
        return new ResponseEntity<>(listingPageFaqService.deleteFaq(id), HttpStatus.OK);
    }
}
