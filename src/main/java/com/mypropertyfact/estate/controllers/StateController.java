package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.StateDto;
import com.mypropertyfact.estate.interfaces.StateService;
import com.mypropertyfact.estate.models.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/state")
@RequiredArgsConstructor
public class StateController {

    private final StateService stateService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllStates() {
        return ResponseEntity.ok(stateService.getAll());
    }

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> addUpdate(@RequestBody StateDto stateDto){
        return ResponseEntity.ok(stateService.addUpdate(stateDto));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Void> deleteState(@PathVariable("id") int id){
        stateService.deleteState(id);
        return ResponseEntity.ok().build();
    }
}
