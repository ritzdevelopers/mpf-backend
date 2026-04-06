package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.ProjectGalleryDto;
import com.mypropertyfact.estate.entities.ProjectGallery;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.ProjectGalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/project-gallery")
@RequiredArgsConstructor
public class ProjectGalleryController {

    private final ProjectGalleryService projectGalleryService;

    @GetMapping("/get-all")
    public ResponseEntity<List<Map<String, Object>>> getAllProjectGallery() {
        return new ResponseEntity<>(this.projectGalleryService.getAllGalleryImages(), HttpStatus.OK);
    }
    @PostMapping("/add-new")
    public ResponseEntity<Response> postGalleryImage(@ModelAttribute ProjectGalleryDto projectGalleryDto){
        return new ResponseEntity<>(this.projectGalleryService.postGalleryImage(projectGalleryDto), HttpStatus.OK);
    }
    @GetMapping("/get/{url}")
    public ResponseEntity<List<ProjectGallery>> getBySlugUrl(@PathVariable("url")String url){
        return new ResponseEntity<>(this.projectGalleryService.getBySlugUrl(url), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> deleteGalleryImage(@PathVariable("id") int id){
        return new ResponseEntity<>(projectGalleryService.deleteGalleryImage(id), HttpStatus.OK);
    }
}
