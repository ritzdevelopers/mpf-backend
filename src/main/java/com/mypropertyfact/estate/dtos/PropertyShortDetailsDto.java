package com.mypropertyfact.estate.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertyShortDetailsDto {
    private Long id;
    private String listingType;
    private String transaction;
    private String subType;
    private String status;
    private String projectName;
    private String builderName;
    private String address;
    private String locality;
    private String city;
    private String pinCode;
    private Double carpetArea;
    private Double builtUpArea;
    private Double totalPrice;
    private Double pricePerSqft;
    private Integer bathrooms;
    private Integer bedrooms;
    private String facing;
    private LocalDateTime createdAt;
    private List<String> imageUrls;

    public PropertyShortDetailsDto(Long id, String listingType, String transaction, String subType, String status,
            String projectName, String builderName, String address, String locality, String city, String pinCode,
            Double carpetArea, Double builtUpArea, Double totalPrice, Double pricePerSqft, Integer bathrooms,
            Integer bedrooms, String facing, LocalDateTime createdAt) {
        this.id = id;
        this.listingType = listingType;
        this.transaction = transaction;
        this.subType = subType;
        this.status = status;
        this.projectName = projectName;
        this.builderName = builderName;
        this.address = address;
        this.locality = locality;
        this.city = city;
        this.pinCode = pinCode;
        this.carpetArea = carpetArea;
        this.builtUpArea = builtUpArea;
        this.totalPrice = totalPrice;
        this.pricePerSqft = pricePerSqft;
        this.bathrooms = bathrooms;
        this.bedrooms = bedrooms;
        this.facing = facing;
        this.createdAt = createdAt;
    }

}
