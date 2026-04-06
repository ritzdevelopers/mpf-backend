package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.CareerApplicationDto;
import com.mypropertyfact.estate.interfaces.CareerApplicationService;
import com.mypropertyfact.estate.models.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/career")
@RequiredArgsConstructor
public class CareerApplicationController {

    private final CareerApplicationService careerApplicationService;

    @GetMapping
    public ResponseEntity<List<CareerApplicationDto>> getAllApplication() {
        return ResponseEntity.ok(careerApplicationService.getAllCareerApplication());
    }

    @PostMapping
    public ResponseEntity<Response> submitApplication(@Valid @ModelAttribute CareerApplicationDto careerApplicationDto){
        return ResponseEntity.ok(careerApplicationService.submitApplication(careerApplicationDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> deleteApplication(@PathVariable Long id){
        return ResponseEntity.ok(careerApplicationService.deleteCareerApplication(id));
    }
}
