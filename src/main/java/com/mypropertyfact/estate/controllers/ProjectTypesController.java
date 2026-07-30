package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.ProjectTypeDto;
import com.mypropertyfact.estate.entities.ProjectTypes;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.projections.ProjectTypeView;
import com.mypropertyfact.estate.services.ProjectTypesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-types")
@RequiredArgsConstructor
public class ProjectTypesController {

    private final ProjectTypesService projectTypesService;
    @GetMapping("/get-all")
    public ResponseEntity<List<ProjectTypeView>> getAllProjectTypes(){
        return new ResponseEntity<>(this.projectTypesService.getAllProjectTypes(), HttpStatus.OK);
    }

    @GetMapping("/get-all-types")
    public ResponseEntity<List<ProjectTypes>> getAllProjectTypesList(){
        return new ResponseEntity<>(this.projectTypesService.getAllProjectTypesList(), HttpStatus.OK);
    }
    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> addUpdateProjectType(@RequestBody ProjectTypes projectTypes){
        return new ResponseEntity<>(this.projectTypesService.addUpdateProjectType(projectTypes), HttpStatus.OK);
    }
    @GetMapping("/get/{url}")
    public ResponseEntity<ProjectTypeDto> getBySlug(@PathVariable("url")String url){
        return new ResponseEntity<>(this.projectTypesService.getBySlug(url), HttpStatus.OK);
    }
//    @GetMapping("/{url}")
//    public ResponseEntity<List<Project>> getPropertiesBySlug(@PathVariable("url")String url){
//        return new ResponseEntity<>(this.projectTypesService.getPropertiesBySlug(url), HttpStatus.OK);
//    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> deleteProjectType(@PathVariable("id")int id){
        return new ResponseEntity<>(this.projectTypesService.deleteProjectType(id), HttpStatus.OK);
    }
}
