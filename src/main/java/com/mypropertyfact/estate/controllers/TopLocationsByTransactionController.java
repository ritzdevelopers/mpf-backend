package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.entities.TopLocationsByTransaction;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.models.TopLocationByTransactionResponse;
import com.mypropertyfact.estate.services.TopLocationsByTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/top-locations-by-transaction")
@RequiredArgsConstructor
public class TopLocationsByTransactionController {
    private final TopLocationsByTransactionService topLocationsByTransactionService;

    @GetMapping("/get")
    public ResponseEntity<List<TopLocationsByTransaction>> getAllTopLocationsByTransaction(){
        return new ResponseEntity<>(topLocationsByTransactionService.getAllTopLocationsByTransaction(), HttpStatus.OK);
    }
    @PostMapping("/post")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<Response> addUpdateTopLocationsByTransaction(@RequestBody TopLocationsByTransaction topLocationsByTransaction){
        return new ResponseEntity<>(topLocationsByTransactionService.addUpdateTopLocationsByTransaction(topLocationsByTransaction), HttpStatus.OK);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<Response> deleteTopLocationsByTransaction(@PathVariable("id")int id){
        return new ResponseEntity<>(topLocationsByTransactionService.deleteTopLocationsByTransaction(id), HttpStatus.OK);
    }

    @GetMapping("/top-location-by-transaction")
    public ResponseEntity<List<TopLocationByTransactionResponse>> getTopLocationByTransaction(){
        return new ResponseEntity<>(topLocationsByTransactionService.getAllCategoryWiseData(), HttpStatus.OK);
    }
}
