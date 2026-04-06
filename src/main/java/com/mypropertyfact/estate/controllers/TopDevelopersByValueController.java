package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.entities.TopDevelopersByValue;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.models.TopDevelopersByValueResponse;
import com.mypropertyfact.estate.services.TopDevelopersByValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/top-developers-by-value")
@RestController
@RequiredArgsConstructor
public class TopDevelopersByValueController {

    private final TopDevelopersByValueService topDevelopersByValueService;

    @GetMapping("/get-all")
    public ResponseEntity<List<TopDevelopersByValue>> getAll(){
        return new ResponseEntity<>(topDevelopersByValueService.getAllTopDevelopersByValue(), HttpStatus.OK);
    }
    @GetMapping("/get")
    public ResponseEntity<List<TopDevelopersByValueResponse>> getTopDevelopersByValue(){
        return new ResponseEntity<>(topDevelopersByValueService.getTopDevelopersByValue(), HttpStatus.OK);
    }
    @PostMapping("/post")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<Response> addUpdateDeveloperData(@RequestBody TopDevelopersByValue topDevelopersByValue){
        return new ResponseEntity<>(topDevelopersByValueService.addUpdateTopDevelopersByValue(topDevelopersByValue),
                HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<Response> deleteDeveloperData(@PathVariable("id")int id){
        return new ResponseEntity<>(topDevelopersByValueService.deleteTopDevelopersByValue(id),
                HttpStatus.OK);
    }

}
