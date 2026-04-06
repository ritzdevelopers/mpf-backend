package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.ProjectBannerDto;
import com.mypropertyfact.estate.dtos.ProjectDetailDto;
import com.mypropertyfact.estate.entities.ProjectBanner;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.ProjectBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-banner")
@RequiredArgsConstructor
public class ProjectBannerController {

    private final ProjectBannerService projectBannerService;

    @Transactional
    @GetMapping("/get-all")
    public ResponseEntity<List<ProjectDetailDto>> getAllBanners(){
        return new ResponseEntity<>(this.projectBannerService.getAllBanners(), HttpStatus.OK);
    }
    @GetMapping("/get/{url}")
    public ResponseEntity<ProjectBanner> getAllMobileBanners(@PathVariable("url")String url){
        return new ResponseEntity<>(this.projectBannerService.getBySlug(url), HttpStatus.OK);
    }
    @PostMapping("/add-banner")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> postBanner(@ModelAttribute ProjectBannerDto projectBannerDto){
        return new ResponseEntity<>(this.projectBannerService.postBanner(projectBannerDto), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> deleteBanner(@PathVariable("id")int id){
        return new ResponseEntity<>(projectBannerService.deleteBanner(id), HttpStatus.OK);
    }
}
