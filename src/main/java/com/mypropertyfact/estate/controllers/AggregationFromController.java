package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.entities.AggregationFrom;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.AggregationFromService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aggregationFrom")
@Slf4j
@RequiredArgsConstructor
public class AggregationFromController {
    private final AggregationFromService aggregationFromService;
    @GetMapping("/get")
    public ResponseEntity<List<AggregationFrom>> getAllAggregationFrom(){
        log.info("get all aggregation function called.");
        return new ResponseEntity<>(aggregationFromService.getAllAggregationFrom(), HttpStatus.OK);
    }
    @PostMapping("/post")
    public ResponseEntity<Response> addUpdateAggregationFrom(@RequestBody AggregationFrom aggregationFrom){
        return new ResponseEntity<>(aggregationFromService.addUpdateAggregationFrom(aggregationFrom), HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_OPTIONS')")
    public ResponseEntity<Response> deleteAggregationFrom(@PathVariable("id")int id){
        return new ResponseEntity<>(aggregationFromService.deleteAggregationFrom(id), HttpStatus.OK);
    }
}
