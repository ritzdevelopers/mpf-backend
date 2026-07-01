package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.AddUpdateProjectDto;
import com.mypropertyfact.estate.dtos.ProjectExportDto;
import com.mypropertyfact.estate.dtos.ProjectShortDetails;
import com.mypropertyfact.estate.models.ProjectAmenityDto;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/projects")
@Slf4j
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectShortDetails>> getShortDetails() {
        return new ResponseEntity<>(projectService.getAllProjects(), HttpStatus.OK);
    }

    /**
     * Full project rows for admin Excel export (ids, SEO, HTML descriptions, RERA, timestamps, etc.).
     */
    @GetMapping("/admin-export")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<List<ProjectExportDto>> adminExportForExcel() {
        return new ResponseEntity<>(projectService.findAllForExcelExport(), HttpStatus.OK);
    }

    @PostMapping("/add-new")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> saveProject(
            @RequestParam(required = false) MultipartFile projectLogo,
            @RequestParam(required = false) MultipartFile locationMap,
            @RequestParam(required = false) MultipartFile projectThumbnail,
            @RequestPart("addUpdateProjectDto") AddUpdateProjectDto addUpdateProjectDto) {
        return new ResponseEntity<>(this.projectService.saveProject(
                projectLogo,
                locationMap,
                projectThumbnail,
                addUpdateProjectDto
        ), HttpStatus.OK);
    }

    @GetMapping("/get/{url}")
    public ResponseEntity<?> getBySlug(@PathVariable("url") String url) {
        return new ResponseEntity<>(this.projectService.getProjectDetailsBySlug(url), HttpStatus.OK);
    }

    @GetMapping("/admin/get/{url}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<?> getBySlugForAdmin(@PathVariable("url") String url) {
        return new ResponseEntity<>(this.projectService.getProjectDetailsBySlugForAdmin(url), HttpStatus.OK);
    }

    @GetMapping("/admin/get-by-id/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<?> getByIdForAdmin(@PathVariable("id") int id) {
        return new ResponseEntity<>(this.projectService.getProjectDetailsByIdForAdmin(id), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> deleteProject(@PathVariable("id") int id) {
        return new ResponseEntity<>(this.projectService.deleteProject(id), HttpStatus.OK);
    }

    @GetMapping("/search-by-type-city-budget")
    public ResponseEntity<?> searchByPropertyTypeLocationBudget(@RequestParam("propertyType") String propertyType,
                                                                @RequestParam("propertyLocation") String propertyLocation,
                                                                @RequestParam("budget") String budget,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "9") int limit) {
        return new ResponseEntity<>(projectService.searchByPropertyTypeLocationBudget(propertyType, propertyLocation, budget), HttpStatus.OK);
    }

    @PostMapping("/add-update-amenity")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> addUpdateAmenity(@RequestBody ProjectAmenityDto projectAmenityDto) {
        return new ResponseEntity<>(projectService.addUpdateAmenity(projectAmenityDto), HttpStatus.OK);
    }

    @GetMapping("/all-floor-types")
    public ResponseEntity<Set<String>> getAllFloorTypes() {
        return ResponseEntity.ok(projectService.getAllFloorTypes());
    }

    @GetMapping("/get-projects-in-parts")
    public ResponseEntity<List<ProjectShortDetails>> getProjectInParts(
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size
    ) {
        return ResponseEntity.ok(projectService.getProjectInParts(page, size));
    }

    @GetMapping("/get-by-slug/{slug}")
    public ResponseEntity<?> getProjectDetailsBySlug(@PathVariable("slug") String slug) {
        return new ResponseEntity<>(projectService.getProjectDetailsBySlug(slug), HttpStatus.OK);
    }
}
