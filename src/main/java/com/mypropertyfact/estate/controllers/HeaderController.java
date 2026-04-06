package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.entities.Headers;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.HeaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/headers")
@RequiredArgsConstructor
public class HeaderController {

    private final HeaderService headerService;

    @GetMapping("/get")
    public ResponseEntity<List<Headers>> getAllHeaders(){
        return new ResponseEntity<>(headerService.getAllHeaders(), HttpStatus.OK);
    }
    @PostMapping("/post")
    public ResponseEntity<Response> addUpdateHeader(@RequestBody Headers headers){
        return new ResponseEntity<>(headerService.addUpdateHeader(headers), HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<Response> deleteHeader(@PathVariable("id")int id){
        return new ResponseEntity<>(headerService.deleteHeader(id), HttpStatus.OK);
    }
}
