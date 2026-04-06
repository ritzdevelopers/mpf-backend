package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.entities.Category;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    @GetMapping("/get")
    public ResponseEntity<List<Category>> getAllCategory(){
        return new ResponseEntity<>(categoryService.getAllCategory(), HttpStatus.OK);
    }
    @PostMapping("/post")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<Response> addUpdateCategory(@RequestBody Category category){
        return new ResponseEntity<>(categoryService.addUpdateCategory(category), HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<Response> deleteCategory(@PathVariable int id){
        return new ResponseEntity<>(categoryService.deleteCategory(id), HttpStatus.OK);
    }
}
