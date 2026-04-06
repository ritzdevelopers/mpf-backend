package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.ProjectStatusDto;
import com.mypropertyfact.estate.interfaces.ProjectStatusService;
import com.mypropertyfact.estate.models.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-status")
@RequiredArgsConstructor
public class ProjectStatusController {

    private final ProjectStatusService projectStatusService;

    // Get all project statuses
    @GetMapping
    public ResponseEntity<List<ProjectStatusDto>> getAllProjectStatus() {
        return ResponseEntity.ok(projectStatusService.getAllStatus());
    }

    // Add or update project status
    @PostMapping
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<?> addUpdateProjectStatus(@RequestBody @Valid ProjectStatusDto projectStatusDto) {
        Response response = projectStatusService.addUpdate(projectStatusDto);
        if(response.getIsSuccess() == 1){
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

}
