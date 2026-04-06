package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.interfaces.DistrictService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/district")
@RequiredArgsConstructor
public class DistrictServiceController {

    private final DistrictService districtService;

    @PostMapping
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<?> addAllIndiaData(@RequestParam("file")MultipartFile multipartFile){
        return ResponseEntity.ok(districtService.addAllDetailsFromFile(multipartFile));
    }
}
