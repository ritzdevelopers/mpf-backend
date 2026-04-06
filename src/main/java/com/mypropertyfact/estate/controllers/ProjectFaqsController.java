package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.ProjectFaqDto;
import com.mypropertyfact.estate.entities.ProjectFaqs;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.ProjectFaqsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/project-faqs")
@RequiredArgsConstructor
public class ProjectFaqsController {

    private final ProjectFaqsService projectFaqsService;

    @GetMapping("/get-all")
    public ResponseEntity<List<Map<String, Object>>> getAllFaq(){
        return new ResponseEntity<>(this.projectFaqsService.getAllFaqs(), HttpStatus.OK);
    }
    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> addUpdateFaq(@RequestBody ProjectFaqDto projectFaqsDto){
        return new ResponseEntity<>(this.projectFaqsService.addUpdateFaqs(projectFaqsDto), HttpStatus.OK);
    }
    @GetMapping("/get/{url}")
    public ResponseEntity<List<ProjectFaqs>> getBySlug(@PathVariable("url")String url){
        return new ResponseEntity<>(this.projectFaqsService.getBySlug(url), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> deleteFaq(@PathVariable("id")int id){
        return new ResponseEntity<>(projectFaqsService.deleteFaq(id), HttpStatus.OK);
    }
}
