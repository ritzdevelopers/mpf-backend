package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.BuilderResponse;
import com.mypropertyfact.estate.dtos.BuilderDto;
import com.mypropertyfact.estate.entities.Builder;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.BuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/builder")
@RequiredArgsConstructor
public class BuilderController {

    private final BuilderService builderService;

    @GetMapping("/get-all")
    public ResponseEntity<BuilderResponse> getAllBuilders() {
        return new ResponseEntity<>(builderService.getAllBuilders(), HttpStatus.OK);
    }

    @GetMapping("/get-all-builders")
    public ResponseEntity<List<Builder>> getAllBuildersList() {
        return ResponseEntity.ok(builderService.getAllBuildersList());
    }

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> addUpdateBuilder(@RequestBody Builder builder) {
        Response response = this.builderService.addUpdateBuilder(builder);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> deleteBuilder(@PathVariable("id") int id){
        return new ResponseEntity<>(this.builderService.deleteBuilder(id), HttpStatus.OK);
    }
    @GetMapping("/get/{url}")
    public ResponseEntity<BuilderDto> getBuilderBySlug(@PathVariable("url") String url){
        return new ResponseEntity<>(this.builderService.getBySlug(url), HttpStatus.OK);
    }
}
