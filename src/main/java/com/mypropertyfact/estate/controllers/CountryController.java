package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.CountryDto;
import com.mypropertyfact.estate.interfaces.CountryService;
import com.mypropertyfact.estate.models.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/country")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @GetMapping("/get-all-countries")
    public ResponseEntity<?> getAllCountries() {
        return ResponseEntity.ok(countryService.getAllCountry());
    }

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> addUpdate(@RequestBody CountryDto countryDto){
        return ResponseEntity.ok(countryService.addUpdate(countryDto));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> deleteCountry(@PathVariable("id") int id){
        return ResponseEntity.ok(countryService.deleteCountry(id));
    }
}
