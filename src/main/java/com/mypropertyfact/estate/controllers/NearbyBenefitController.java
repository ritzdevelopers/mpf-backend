package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.NearbyBenefitDetailedDto;
import com.mypropertyfact.estate.entities.MasterBenefit;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.NearbyBenefitService;
import com.mypropertyfact.estate.services.ProjectNearbyBenefitsExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nearby-benefit")
@RequiredArgsConstructor
public class NearbyBenefitController {

    private final NearbyBenefitService nearbyBenefitService;
    private final ProjectNearbyBenefitsExcelService projectNearbyBenefitsExcelService;
    
    @GetMapping("/get-all")
    public ResponseEntity<List<MasterBenefit>> getAllNearbyBenefits() {
        return new ResponseEntity<>(nearbyBenefitService.getAllNearbyBenefits(), HttpStatus.OK);
    }
    
    @PostMapping("/post-multiple-nearby-benefits")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_NEARBY_BENEFITS')")
    public ResponseEntity<Response> postMultipleNearbyBenefits(@ModelAttribute NearbyBenefitDetailedDto dto) {
        Response response = nearbyBenefitService.postMultipleNearbyBenefits(dto);
        if (response.getIsSuccess() == 1) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Bulk map location benefits per project from Excel (same format as {@code /api/v1/excel-upload/nearby-benefits}).
     * Matches each row to a project by name (Project column). Optional {@code replaceExisting=true} replaces
     * existing location benefits for that project.
     */
    @PostMapping("/upload-projects-location-benefits-excel")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_NEARBY_BENEFITS')")
    public ResponseEntity<Response> uploadProjectsLocationBenefitsExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replaceExisting", defaultValue = "false") boolean replaceExisting) {
        Response response = projectNearbyBenefitsExcelService.uploadNearbyBenefitsExcel(file, replaceExisting);
        if (response.getIsSuccess() == 1) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_NEARBY_BENEFITS')")
    public ResponseEntity<Response> deleteNearbyBenefit(@PathVariable("id") Integer id) {
        return new ResponseEntity<>(nearbyBenefitService.deleteNearbyBenefit(id), HttpStatus.OK);
    }
}
