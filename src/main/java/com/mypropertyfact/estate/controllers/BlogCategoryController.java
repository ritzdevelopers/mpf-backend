package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.BlogCategoryDto;
import com.mypropertyfact.estate.entities.BlogCategory;
import com.mypropertyfact.estate.interfaces.BlogCategoryService;
import com.mypropertyfact.estate.models.ResourceNotFoundException;
import com.mypropertyfact.estate.models.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blog-category")
@RequiredArgsConstructor
public class BlogCategoryController {
    private final BlogCategoryService blogCategoryService;

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_BLOGS')")
    public ResponseEntity<Response> addUpdateBlogCategory(@RequestBody BlogCategory blogCategory){
        return ResponseEntity.ok(blogCategoryService.addUpdateBlogCategory(blogCategory));
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<BlogCategoryDto>> getAll(){
        return ResponseEntity.ok(blogCategoryService.getAllCategories());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<BlogCategory> getBlogCategoryById(@PathVariable("id") int id){
        return ResponseEntity.ok(blogCategoryService.getBlogCategoryById(id).orElseThrow(
                ()-> new ResourceNotFoundException("Blog category not found with this category id: "+ id)));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_BLOGS')")
    public ResponseEntity<Response> deleteBlogCategory(@PathVariable("id")int id){
        return ResponseEntity.ok(blogCategoryService.deleteBlogCategory(id));
    }
}
