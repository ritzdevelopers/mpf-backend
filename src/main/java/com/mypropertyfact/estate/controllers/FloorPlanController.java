package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.FloorPlanDto;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.FloorPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/floor-plans")
@RequiredArgsConstructor
public class FloorPlanController {

    private final FloorPlanService floorPlanService;

    @GetMapping("/get-all")
    public ResponseEntity<List<Map<String, Object>>> getAllFloorPlans(){
        return new ResponseEntity<>(this.floorPlanService.getAllPlans(), HttpStatus.OK);
    }

    @PostMapping("/add-update")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> addUpdateFloorPlan(@RequestBody FloorPlanDto floorPlan){
        return new ResponseEntity<>(this.floorPlanService.addUpdatePlan(floorPlan), HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROJECTS')")
    public ResponseEntity<Response> deleteFloorPlan(@PathVariable("id")int id){
        return new ResponseEntity<>(this.floorPlanService.deleteFloorPlan(id), HttpStatus.OK);
    }
}
