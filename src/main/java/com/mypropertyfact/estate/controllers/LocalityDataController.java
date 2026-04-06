package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.LocalityDataResponse;
import com.mypropertyfact.estate.entities.LocalityData;
import com.mypropertyfact.estate.interfaces.LocalityDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/api/v1/locality-data")
@RestController
@RequiredArgsConstructor
public class LocalityDataController {

    private final LocalityDataService service;

    @PostMapping("/save")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<String> saveLocalityData(@RequestBody Map<String, Object> payload) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            LocalityData data = new LocalityData();

            data.setLocalityId((String) payload.get("localityId"));
            data.setLocalEconomyList(mapper.writeValueAsString(payload.get("localEconomyList")));
            data.setLocalEconomyScore(Integer.parseInt((String) payload.get("localEconomyScore")));

            data.setOnGoingFutureProjectsList(mapper.writeValueAsString(payload.get("onGoingFutureProjectsList")));
            data.setOnGoingFutureProjectsScore(Integer.parseInt((String) payload.get("onGoingFutureProjectsScore")));

            data.setConnectivityAndCommuteList(mapper.writeValueAsString(payload.get("connectivityAndCommuteList")));
            data.setConnectivityAndCommuteScore(Integer.parseInt((String) payload.get("connectivityAndCommuteScore")));

            data.setAmenitiesAndGentrificationList(mapper.writeValueAsString(payload.get("amenitiesAndGentrificationList")));
            data.setAmenitiesAndGentrificationScore(Integer.parseInt((String) payload.get("amenitiesAndGentrificationScore")));

            data.setTrendsAndHistoricalDataList(mapper.writeValueAsString(payload.get("trendsAndHistoricalDataList")));
            data.setTrendsAndHistoricalDataScore(Integer.parseInt((String) payload.get("trendsAndHistoricalDataScore")));

            data.setExistingSupplyList(mapper.writeValueAsString(payload.get("existingSupplyList")));
            data.setExistingSupplyScore(Integer.parseInt((String) payload.get("existingSupplyScore")));

            data.setInterpretationAndOutlookList(mapper.writeValueAsString(payload.get("interpretationAndOutlookList")));
            data.setRecommendationsForInvestorsList(mapper.writeValueAsString(payload.get("recommendationsForInvestorsList")));

            service.saveLocalityData(data);

            return ResponseEntity.ok("Saved successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{localityId}")
    public ResponseEntity<?> getLocalityData(@PathVariable String localityId) {
        try {
            LocalityDataResponse response = service.getByLocalityId(localityId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllLocalities() {
        try {
            List<LocalityDataResponse> response = service.getAllLocalities();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
