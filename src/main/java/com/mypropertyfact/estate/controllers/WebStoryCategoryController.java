package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.WebStoryCategoryDto;
import com.mypropertyfact.estate.interfaces.WebStoryCategoryService;
import com.mypropertyfact.estate.models.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/web-story-category")
@RequiredArgsConstructor
public class WebStoryCategoryController {

    private final WebStoryCategoryService webStoryCategoryService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllCategories() {
        return ResponseEntity.ok(webStoryCategoryService.getAllCategories());
    }

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_WEB_STORIES')")
    public ResponseEntity<?> addUpdate(@RequestBody WebStoryCategoryDto webStoryCategoryDto) {
        return ResponseEntity.ok(webStoryCategoryService.addUpdate(webStoryCategoryDto));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_WEB_STORIES')")
    public ResponseEntity<Response> deleteCategory(@PathVariable("id") int id) {
        return ResponseEntity.ok(webStoryCategoryService.deleteCategory(id));
    }
}
