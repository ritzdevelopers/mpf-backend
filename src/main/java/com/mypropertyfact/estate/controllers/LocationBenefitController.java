package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.LocationBenefitDto;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.LocationBenefitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/location-benefit")
@RequiredArgsConstructor
public class LocationBenefitController {

    private final LocationBenefitService locationBenefitService;

    @GetMapping("/get-all")
    public ResponseEntity<List<Map<String, Object>>> getAllBenefits(){
        return new ResponseEntity<>(this.locationBenefitService.getAllBenefits(), HttpStatus.OK);
    }
    @PostMapping("/add-new")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> addUpdate(@RequestParam(required = false) MultipartFile iconImage, @ModelAttribute LocationBenefitDto locationBenefitDto){
        return new ResponseEntity<>(this.locationBenefitService.addUpdateBenefit(iconImage, locationBenefitDto), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> deleteBenefit(@PathVariable("id")int id){
        return new ResponseEntity<>(this.locationBenefitService.deleteLocationBenefit(id), HttpStatus.OK);
    }

}
