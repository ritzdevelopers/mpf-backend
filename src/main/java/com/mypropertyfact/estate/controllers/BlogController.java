package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.BlogDto;
import com.mypropertyfact.estate.entities.Blog;
import com.mypropertyfact.estate.interfaces.BlogService;
import com.mypropertyfact.estate.models.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blog")
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_BLOGS')")
    public ResponseEntity<Response> addUpdate(@RequestParam(required = false) MultipartFile image, @Valid @ModelAttribute BlogDto blogDto) {
        return new ResponseEntity<>(blogService.addUpdateBlog(image, blogDto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_BLOGS')")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_BLOGS')")
    public ResponseEntity<Response> updateStatus(
            @PathVariable int id,
            @RequestParam int status) {
        return ResponseEntity.ok(blogService.updateBlogStatus(id, status));
    }

    @PostMapping("/update-status")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_BLOGS')")
    public ResponseEntity<Response> updateStatusPost(
            @RequestParam int id,
            @RequestParam int status) {
        return ResponseEntity.ok(blogService.updateBlogStatus(id, status));
    }

    @GetMapping("/get-all")
    public List<BlogDto> getAll() {
        return blogService.getAllBlogs();
    }

    @GetMapping("/get/{slug}")
    public ResponseEntity<BlogDto> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(blogService.getBySlug(slug));
    }

    @GetMapping("/get")
    public Page<BlogDto> getWithPagination(
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "9")int size,
            @RequestParam(defaultValue = "blog")String from,
            @RequestParam(required = false) String search
    ){
        return blogService.getWithPagination(page, size, from, search);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Blog> getOne(@PathVariable int id) {
        return blogService.getBlogById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found"));
    }
}

