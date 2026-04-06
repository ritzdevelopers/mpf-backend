package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.ProjectWalkthroughDto;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.ProjectWalkthroughService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-walkthrough")
@RequiredArgsConstructor
public class ProjectWalkthroughController {

    private final ProjectWalkthroughService projectWalkthroughService;

    @GetMapping("/get")
    public ResponseEntity<List<ProjectWalkthroughDto>> getAllWalkthrough(){
        return new ResponseEntity<>(this.projectWalkthroughService.getAllWalkthrough(), HttpStatus.OK);
    }
    @PostMapping("/add-update")
    public ResponseEntity<Response> addUpdate(@RequestBody ProjectWalkthroughDto projectWalkthroughDto){
        return new ResponseEntity<>(this.projectWalkthroughService.addUpdate(projectWalkthroughDto), HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> deleteWalkthrough(@PathVariable("id") int id){
        return new ResponseEntity<>(this.projectWalkthroughService.deleteWalkthrough(id), HttpStatus.OK);
    }
}
