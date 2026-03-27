package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.ExcelUploadService;
import com.mypropertyfact.estate.services.ProjectExcelUploadService;
import com.mypropertyfact.estate.services.ProjectNearbyBenefitsExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/excel-upload")
@RequiredArgsConstructor
public class ExcelUploadController {

    private final ExcelUploadService excelUploadService;

    @Autowired
    private ProjectExcelUploadService projectExcelUploadService;

    @Autowired
    private ProjectNearbyBenefitsExcelService projectNearbyBenefitsExcelService;

    @PostMapping("/city-zone-locality")
    public ResponseEntity<Response> uploadCityZoneLocality(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "defaultProjectTypeId", required = false) Integer defaultProjectTypeId) {
        return ResponseEntity.ok(excelUploadService.uploadCityZoneLocalityExcel(file, defaultProjectTypeId));
    }

    /**
     * Bulk upload projects from Excel. Excel must have header row matching column names
     * (e.g. S NO, BUILDER NAME, PROJECT NAME, PROPERTY TYPE, PROJECT STATUS NAME, PROJECT CONFIGURATION,
     * PROJECT LOCALITY, CITY NAME, STATE NAME, COUNTRY NAME, RERA NO, PROJECT PRICE, AMENITIES,
     * FLOOR PLAN DESCRIPTION, LOCATION MAP DESCRIPTION, ABOUT PROJECT DESCRIPTION, BUILDER DESCRIPTION,
     * PROJECT THUMBNAIL IMAGE, LOCATION MAP IMAGE, PROJECT FEATURE IMAGE, GALLERY IMAGE 1 ... 16).
     * Optional: upload a zip file containing images; Excel image columns should contain the file name
     * (e.g. "thumb.jpg") that exists inside the zip.
     */
    @PostMapping("/projects")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Response> uploadProjects(
            @RequestParam("file") MultipartFile excelFile,
            @RequestParam(value = "imagesZip", required = false) MultipartFile imagesZip) {
        return ResponseEntity.ok(projectExcelUploadService.uploadProjectsExcel(excelFile, imagesZip));
    }

    /**
     * Bulk update nearby benefits (location benefits) for projects that do not yet have any.
     * Excel structure: S.no, Project, School, Malls/ IT Park, Hospitals, Roads/ Highway,
     * Famous for/ Metro, Airport/Famous places. Each benefit cell: Place-Name_Distance-Km
     * (e.g. GD-Goenka-International-School_7-Km). Only projects with no existing location benefits are updated.
     */
    @PostMapping("/nearby-benefits")
//    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Response> uploadNearbyBenefits(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(projectNearbyBenefitsExcelService.uploadNearbyBenefitsExcel(file));
    }
}
