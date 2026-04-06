package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.AmenityDto;
import com.mypropertyfact.estate.dtos.AmenityDetailedDto;
import com.mypropertyfact.estate.entities.Amenity;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/amenity")
@RequiredArgsConstructor
public class AmenityController {
    private final AmenityService amenityService;

    @GetMapping("/get-all")
    public ResponseEntity<List<Amenity>> getAllAmenities(){
        return new ResponseEntity<>(this.amenityService.getAllAmenities(), HttpStatus.OK);
    }

    @PostMapping("/post")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_AMENITIES')")
    public ResponseEntity<Response> postNewAmenity(@RequestParam(required = false) MultipartFile amenityImage,
                                                   @ModelAttribute AmenityDto amenityDto){
        Response response = this.amenityService.postAmenity(amenityImage, amenityDto);
        if(response.getIsSuccess() == 1){
           return new ResponseEntity<>(response, HttpStatus.OK);
        }else{
           return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_AMENITIES')")
    public ResponseEntity<Response> deleteAmenity(@PathVariable("id") int id){
        return new ResponseEntity<>(this.amenityService.deleteAmenity(id), HttpStatus.OK);
    }

    @PostMapping("/post-multiple-amenities")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_AMENITIES')")
    public ResponseEntity<Response> postMultipleAmenities(@ModelAttribute AmenityDetailedDto dto) {
        return ResponseEntity.ok(amenityService.postMultipleAmenities(dto));
    }

    @GetMapping("/get-by-project-id/{projectId}")
    public ResponseEntity<List<Amenity>> getAmenitiesByProjectId(@PathVariable("projectId") int projectId){
        return new ResponseEntity<>(this.amenityService.getAmenitiesByProjectId(projectId), HttpStatus.OK);
    }
}
