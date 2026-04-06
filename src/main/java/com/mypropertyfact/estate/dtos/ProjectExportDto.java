package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Flat row for admin Excel export — matches core {@code Project} scalars and related IDs/names.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectExportDto {
    private int id;
    private String projectName;
    private String slugURL;
    private Integer builderId;
    private String builderName;
    private Integer cityId;
    private String cityName;
    private String stateName;
    private String countryName;
    private Integer countryId;
    private Integer stateId;
    private Integer propertyTypeId;
    private String propertyTypeName;
    private Integer projectStatusId;
    private String projectStatusName;
    private boolean status;
    private boolean showFeaturedProperties;
    private String projectLocality;
    private String projectConfiguration;
    private String projectPrice;
    private String ivrNo;
    private String reraNo;
    private String reraQr;
    private String reraWebsite;
    private String locationMap;
    private String projectLogo;
    private String projectThumbnail;
    private String projectThumbnailAltTag;
    private String projectLogoAltTag;
    private String locationMapAltTag;
    private String metaTitle;
    private String metaKeyword;
    private String metaDescription;
    private String amenityDesc;
    private String floorPlanDesc;
    private String locationDesc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String projectBannerImage;
}
