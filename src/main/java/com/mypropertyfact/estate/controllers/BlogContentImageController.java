package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.entities.BlogContentImage;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.interfaces.BlogContentImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blog-image")
@RequiredArgsConstructor
public class BlogContentImageController {

    private final BlogContentImageService blogContentImageService;

    @GetMapping("/get")
    public List<BlogContentImage> getAllContentImageList() {
        return this.blogContentImageService.getAllImageList();
    }

    @PostMapping("/post")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_BLOGS')")
    public ResponseEntity<Response> addUpdateContentImage(@RequestParam(required = false)
                                                          MultipartFile file,
                                                          @ModelAttribute BlogContentImage blogContentImage) {
        return new ResponseEntity<>(blogContentImageService.addUpdateContentImage(file, blogContentImage), HttpStatus.OK);
    }
}
