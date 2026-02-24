package com.mypropertyfact.estate.dtos;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Setter
@Getter
@Builder
public class ProjectFullDetails {
    private int id;
    private String metaTitle;
    private String metaKeyword;
    private String metaDescription;
    private String projectName;
    private String projectLocality;
    private String projectConfiguration;
    private String projectPrice;
    private String ivrNo;
    private String locationMap;
    private String reraNo;
    private String reraQr;
    private String reraWebsite;
    private String projectLogo;
    private String slugURL;
    private boolean showFeaturedProperties;
    private String projectLogoAltTag;
    private String locationMapAltTag;
    private boolean status;
    private String amenityDesc;
    private String floorPlanDesc;
    private String locationDesc;

    private List<AmenityDto> amenities;
    private List<ProjectDesktopBannerDto> desktopImages;
    private List<ProjectMobileBannerDto> mobileImages;
    private List<FloorPlanDto> floorPlans;
    private BuilderDto builder;
    private List<ProjectGalleryShortDetails> galleryImages;
    private List<LocationBenefitDto> locationBenefits;
    private List<ProjectFaqDto> faqs;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String projectWalkthroughDescription;
    private String state;
    private String city;
    private String country;
    private String propertyTypeName;

    public ProjectFullDetails(
            int id,
            String metaTitle,
            String metaKeyword,
            String metaDescription,
            String projectName,
            String projectLocality,
            String projectConfiguration,
            String projectPrice,
            String ivrNo,
            String locationMap,
            String reraNo,
            String reraQr,
            String reraWebsite,
            String projectLogo,
            String slugURL,
            boolean showFeaturedProperties,
            String projectLogoAltTag,
            String locationMapAltTag,
            boolean status,
            String amenityDesc,
            String floorPlanDesc,
            String locationDesc,
            BuilderDto builder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String projectWalkthroughDescription,
            String state,
            String city,
            String country,
            String propertyTypeName
    ) {
        this.id = id;
        this.metaTitle = metaTitle;
        this.metaKeyword = metaKeyword;
        this.metaDescription = metaDescription;
        this.projectName = projectName;
        this.projectLocality = projectLocality;
        this.projectConfiguration = projectConfiguration;
        this.projectPrice = projectPrice;
        this.ivrNo = ivrNo;
        this.locationMap = locationMap;
        this.reraNo = reraNo;
        this.reraQr = reraQr;
        this.reraWebsite = reraWebsite;
        this.projectLogo = projectLogo;
        this.slugURL = slugURL;
        this.showFeaturedProperties = showFeaturedProperties;
        this.projectLogoAltTag = projectLogoAltTag;
        this.locationMapAltTag = locationMapAltTag;
        this.status = status;
        this.amenityDesc = amenityDesc;
        this.floorPlanDesc = floorPlanDesc;
        this.locationDesc = locationDesc;
        this.builder = builder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.projectWalkthroughDescription = projectWalkthroughDescription;
        this.state = state;
        this.city = city;
        this.country = country;
        this.propertyTypeName = propertyTypeName;
    }
}
