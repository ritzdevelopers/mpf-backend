package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.FeatureDto;
import com.mypropertyfact.estate.dtos.FeatureDetailedDto;
import com.mypropertyfact.estate.entities.Feature;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feature")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;
    
    @GetMapping("/get-all")
    public ResponseEntity<List<Feature>> getAllFeatures() {
        return new ResponseEntity<>(featureService.getAllFeatures(), HttpStatus.OK);
    }
    
    @PostMapping("/post")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_FEATURES')")
    public ResponseEntity<Response> postFeature(@RequestBody FeatureDto featureDto) {
        Response response = featureService.postFeature(featureDto);
        if (response.getIsSuccess() == 1) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/post-multiple-features")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_FEATURES')")
    public ResponseEntity<Response> postMultipleFeatures(@ModelAttribute FeatureDetailedDto dto) {
        return ResponseEntity.ok(featureService.postMultipleFeatures(dto));
    }
    
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_FEATURES')")
    public ResponseEntity<Response> deleteFeature(@PathVariable("id") Long id) {
        return new ResponseEntity<>(featureService.deleteFeature(id), HttpStatus.OK);
    }
}

