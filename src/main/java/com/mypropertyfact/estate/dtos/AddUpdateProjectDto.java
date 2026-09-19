package com.mypropertyfact.estate.dtos;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AddUpdateProjectDto {
    private int id;
    private int builderId;
    private int stateId;
    private Integer cityId;
    private int countryId;
    private int propertyTypeId;
    private int projectStatusId;
    private String projectPrice;
    private String metaTitle;
    private String metaKeyword;
    private String metaDescription;
    private String projectName;
    private String slugURL;
    private String projectLocality;
    private String projectConfiguration;
    private String ivrNo;
    private String reraQr;
    private String reraNo;
    private String reraWebsite;
    private String amenityDescription;
    private String locationDescription;
    private String floorPlanDescription;
    private MultipartFile projectLogo;
    private MultipartFile projectThumbnail;
    private MultipartFile locationMap;
    private boolean showFeaturedProperties;
    private boolean status;
}
