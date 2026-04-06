package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.entities.Amenity;
import com.mypropertyfact.estate.models.ProjectAmenityDto;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.ProjectAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-amenity")
@RequiredArgsConstructor
public class ProjectAmenityController {

    private final ProjectAmenityService projectAmenityService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllProjectAmenity(){
        return new ResponseEntity<>(this.projectAmenityService.getAllProjectAmenity(), HttpStatus.OK);
    }
    @PostMapping("/add-update")
    public ResponseEntity<Response> postAmenity(@RequestBody ProjectAmenityDto projectAmenityDto){
        return new ResponseEntity<>(this.projectAmenityService.addProjectAmenity(projectAmenityDto), HttpStatus.OK);
    }
    @GetMapping("/get/{url}")
    public ResponseEntity<List<Amenity>> getBySlug(@PathVariable("url")String url){
        return new ResponseEntity<>(this.projectAmenityService.getBySlug(url), HttpStatus.OK);
    }
    @GetMapping("/{id}")
    public ResponseEntity<List<Amenity>> getById(@PathVariable("id")int id){
        return new ResponseEntity<>(this.projectAmenityService.getById(id), HttpStatus.OK);
    }
    @DeleteMapping("/delete/{projectId}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> deleteProjectAmenity(@PathVariable("projectId")int projectId){
        return new ResponseEntity<>(this.projectAmenityService.deleteProjectAmenity(projectId), HttpStatus.OK);
    }
}
