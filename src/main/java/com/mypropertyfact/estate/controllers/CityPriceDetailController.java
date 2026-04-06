package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.entities.CityPriceDetail;
import com.mypropertyfact.estate.models.CityPriceDataResponse;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.CityPriceDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/city-price-detail")
@RequiredArgsConstructor
public class CityPriceDetailController {

    private final CityPriceDetailService cityPriceDetailService;

    @GetMapping("/get")
    public ResponseEntity<List<CityPriceDataResponse>> getAllCityPriceDetail(){
        return new ResponseEntity<>(cityPriceDetailService.getAllCityPriceDetail(), HttpStatus.OK);
    }
    @PostMapping("/post")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<Response> addUpdateCityPriceDetail(@RequestBody CityPriceDetail cityPriceDetail){
        return new ResponseEntity<>(cityPriceDetailService.addUpdateCityPriceDetail(cityPriceDetail), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_INSIGHTS')")
    public ResponseEntity<Response> deleteCityPriceDetail(@PathVariable int id){
        return new ResponseEntity<>(cityPriceDetailService.deleteCityPriceDetail(id), HttpStatus.OK);
    }
    @GetMapping("/get-city-price")
    public ResponseEntity<?> getCityPriceData(){
        return new ResponseEntity<>(cityPriceDetailService.cityPriceData(), HttpStatus.OK);
    }
    @GetMapping("/get-top-gainers")
    public ResponseEntity<?> topGainersLocations(){
        return new ResponseEntity<>(cityPriceDetailService.topGainersLocations(), HttpStatus.OK);
    }
}
