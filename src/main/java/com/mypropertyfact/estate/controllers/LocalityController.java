package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.LocalityDto;
import com.mypropertyfact.estate.interfaces.LocalityService;
import com.mypropertyfact.estate.models.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locality")
@RequiredArgsConstructor
public class LocalityController {

    private final LocalityService localityService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllLocalities() {
        return ResponseEntity.ok(localityService.getAllLocalities());
    }

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> addUpdate(@RequestBody LocalityDto localityDto){
        return ResponseEntity.ok(localityService.addUpdateLocality(localityDto));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<?> deleteLocality(@PathVariable("id") long id) {
        return ResponseEntity.ok(localityService.deleteLocality(id));
    }
}
