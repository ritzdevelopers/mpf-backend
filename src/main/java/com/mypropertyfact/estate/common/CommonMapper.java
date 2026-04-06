package com.mypropertyfact.estate.common;

import com.mypropertyfact.estate.dtos.*;
import com.mypropertyfact.estate.entities.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CommonMapper {

    public void mapProjectToProjectDto(Project entity, ProjectDetailDto dto) {
        if (entity != null) {
            dto.setId(entity.getId());
            Builder builder = entity.getBuilder();
            if (builder != null) {
                dto.setBuilderId(builder.getId());
                dto.setBuilderName(builder.getBuilderName());
            }
            City city = entity.getCity();
            if (city != null) {
                dto.setCityId(city.getId());
                dto.setCityName(city.getName());
                dto.setProjectAddress(entity.getProjectLocality().concat(", ").concat(city.getName()));
            }
            ProjectTypes projectTypes = entity.getProjectTypes();
            if (projectTypes != null) {
                dto.setPropertyTypeId(projectTypes.getId());
                dto.setPropertyTypeName(projectTypes.getProjectTypeName());
            }
            ProjectStatus projectStatus = entity.getProjectStatus();
            if (projectStatus != null) {
                dto.setProjectStatusId(projectStatus.getId());
                dto.setProjectStatusName(projectStatus.getStatusName());
            }
            dto.setProjectPrice(entity.getProjectPrice());
            dto.setMetaTitle(entity.getMetaTitle());
            dto.setMetaKeyword(entity.getMetaKeyword());
            dto.setMetaDescription(entity.getMetaDescription());
            dto.setProjectName(entity.getProjectName());
            dto.setSlugURL(entity.getSlugURL());
            dto.setProjectLocality(entity.getProjectLocality());
            dto.setProjectConfiguration(entity.getProjectConfiguration());
            dto.setIvrNo(entity.getIvrNo());
            dto.setReraNo(entity.getReraNo());
            dto.setReraQr(entity.getReraQr());
            dto.setReraWebsite(entity.getReraWebsite());
            dto.setShowFeaturedProperties(entity.isShowFeaturedProperties());
            dto.setAmenityDescription(entity.getAmenityDesc());
            dto.setFloorPlanDescription(entity.getFloorPlanDesc());
            dto.setLocationDescription(entity.getLocationDesc());
            dto.setStatus(entity.isStatus());
            dto.setProjectThumbnailImage(entity.getProjectThumbnail());
            dto.setLocationMapImage(entity.getLocationMap());
            dto.setProjectLogoImage(entity.getProjectLogo());
        }
    }

    public void mapFullProjectDetailToDetailedDto(Project project, ProjectDetailDto detailDto) {
        detailDto.setId(project.getId());
        detailDto.setProjectName(project.getProjectName());
        detailDto.setMetaTitle(project.getMetaTitle());
        detailDto.setMetaDescription(project.getMetaDescription());
        detailDto.setMetaKeyword(project.getMetaKeyword());
        detailDto.setProjectPrice(project.getProjectPrice());
        detailDto.setReraNo(project.getReraNo());
        detailDto.setAmenityDescription(project.getAmenityDesc());
        detailDto.setFloorPlanDescription(project.getFloorPlanDesc());
        detailDto.setLocationDescription(project.getLocationDesc());
        detailDto.setLocationMapImage(project.getLocationMap());
        detailDto.setProjectThumbnailImage(project.getProjectThumbnail());
        detailDto.setProjectLogoImage(project.getProjectLogo());
        detailDto.setSlugURL(project.getSlugURL());
        detailDto.setProjectLocality(project.getProjectLocality());
        detailDto.setProjectConfiguration(project.getProjectConfiguration());
        detailDto.setIvrNo(project.getIvrNo());
        detailDto.setReraQr(project.getReraQr());
        detailDto.setReraWebsite(project.getReraWebsite());
        detailDto.setStatus(project.isStatus());
        ProjectStatus projectStatus = project.getProjectStatus();
        if (projectStatus != null) {
            detailDto.setProjectStatusId(projectStatus.getId());
            detailDto.setProjectStatusName(projectStatus.getStatusName());
        }
        if (project.getBuilder() != null) {
            Builder builder = project.getBuilder();
            detailDto.setBuilderId(builder.getId());
            detailDto.setBuilderName(builder.getBuilderName());
            detailDto.setBuilderDescription(builder.getBuilderDesc());
            detailDto.setBuilderSlugURL(builder.getSlugUrl());
        }
        if (project.getProjectTypes() != null) {
            ProjectTypes projectTypes = project.getProjectTypes();
            detailDto.setPropertyTypeId(projectTypes.getId());
            detailDto.setPropertyTypeName(projectTypes.getProjectTypeName());
        }
        if (project.getCity() != null) {
            City city = project.getCity();
            detailDto.setCityId(city.getId());
            detailDto.setCityName(city.getName());
            detailDto.setProjectAddress(project.getProjectLocality().concat(", ") + city.getName());
            if (city.getState() != null) {
                State state = city.getState();
                detailDto.setStateName(state.getStateName());
                detailDto.setStateId(state.getId());
                if (state.getCountry() != null) {
                    Country country = state.getCountry();
                    detailDto.setCountryId(country.getId());
                    detailDto.setCountryName(country.getCountryName());
                }
            }
        }
        if (project.getProjectsAbout() != null) {
            ProjectsAbout projectsAbout = project.getProjectsAbout();
            detailDto.setProjectAboutShortDescription(projectsAbout.getShortDesc());
            detailDto.setProjectAboutLongDescription(projectsAbout.getLongDesc());
        }
        if (project.getProjectWalkthrough() != null) {
            ProjectWalkthrough projectWalkthrough = project.getProjectWalkthrough();
            detailDto.setProjectWalkthroughDescription(projectWalkthrough.getWalkthroughDesc());
        }
        List<FloorPlanDto> floorPlanList = new ArrayList<>();
        if (project.getFloorPlans() != null) {
            floorPlanList = project.getFloorPlans().stream()
                    .sorted(Comparator.comparing(FloorPlan::getPlanType, String::compareToIgnoreCase))
                    .filter(Objects::nonNull)
                    .map(floorPlan -> {
                        FloorPlanDto floorPlanDto = new FloorPlanDto();
                        floorPlanDto.setProjectId(project.getId());
                        floorPlanDto.setPName(project.getProjectName());
                        floorPlanDto.setFloorId(floorPlan.getId());
                        floorPlanDto.setPlanType(floorPlan.getPlanType());
                        floorPlanDto.setAreaSqMt(floorPlan.getAreaSqmt());
                        floorPlanDto.setAreaSqFt(floorPlan.getAreaSqft());
                        return floorPlanDto;
                    }).toList();
        }
        detailDto.setProjectFloorPlanList(floorPlanList);
        List<ProjectBannerDto> bannerList = new ArrayList<>();
        if (project.getProjectBanners() != null) {
            bannerList = project.getProjectBanners().stream().filter(Objects::nonNull)
                    .map(projectBanner -> {
                        ProjectBannerDto projectBannerDto = new ProjectBannerDto();
                        if (projectBanner.getDesktopBanner() != null) {
                            projectBannerDto.setProjectId(project.getId());
                            projectBannerDto.setProjectName(project.getProjectName());
                            projectBannerDto.setSlugURL(project.getSlugURL());
                            projectBannerDto.setAltTag(projectBanner.getAltTag());
                            projectBannerDto.setId(projectBanner.getId());
                            projectBannerDto.setProjectDesktopBanner(projectBanner.getDesktopBanner());
                            projectBannerDto.setProjectDesktopBannerImageList(new ArrayList<>());
                            projectBannerDto.setProjectMobileBannerImageList(new ArrayList<>());
                            projectBannerDto.setDeletedDesktopImageIds(new ArrayList<>());
                            projectBannerDto.setDeletedMobileImageIds(new ArrayList<>());
                        }
                        if (projectBanner.getMobileBanner() != null) {
                            projectBannerDto.setProjectMobileBanner(projectBanner.getMobileBanner());
                        }
                        return projectBannerDto;
                    }).toList();
        }
        detailDto.setProjectBannerList(bannerList);
        List<AmenityDto> amenityList = new ArrayList<>();
        if (project.getAmenities() != null) {
            amenityList = project.getAmenities().stream().filter(Objects::nonNull)
                    .map(amenity -> {
                        AmenityDto amenityDto = new AmenityDto();
                        amenityDto.setId(amenity.getId());
                        amenityDto.setTitle(amenity.getTitle());
                        amenityDto.setImage(amenity.getAmenityImageUrl());
                        amenityDto.setAltTag(amenity.getAltTag());
                        return amenityDto;
                    }).toList();
        }
        detailDto.setProjectAmenityList(amenityList);

        List<ProjectGalleryDto> projectGalleryList = new ArrayList<>();
        if (project.getProjectGalleries() != null) {
            projectGalleryList = project.getProjectGalleries().stream().filter(Objects::nonNull)
                    .map(projectGallery -> {
                        ProjectGalleryDto projectGalleryDto = new ProjectGalleryDto();
                        projectGalleryDto.setProjectId(project.getId());
                        projectGalleryDto.setDeletedImageIds(new ArrayList<>());
                        projectGalleryDto.setGalleryImageList(new ArrayList<>());
                        projectGalleryDto.setId(projectGallery.getId());
                        projectGalleryDto.setGalleyImage(projectGallery.getImage());
                        return projectGalleryDto;
                    }).toList();
        }
        detailDto.setProjectGalleryImageList(projectGalleryList);
        List<LocationBenefitDto> locationBenefitList = new ArrayList<>();
        if (project.getLocationBenefits() != null) {
            locationBenefitList = project.getLocationBenefits().stream().filter(Objects::nonNull)
                    .map(locationBenefit -> {
                        LocationBenefitDto locationBenefitDto = new LocationBenefitDto();
                        locationBenefitDto.setId(locationBenefit.getId());
                        locationBenefitDto.setProjectName(project.getProjectName());
                        locationBenefitDto.setProjectId(project.getId());
                        locationBenefitDto.setSlugUrl(project.getSlugURL());
                        locationBenefitDto.setImage(locationBenefit.getIconImage());
                        locationBenefitDto.setBenefitName(locationBenefit.getBenefitName());
                        locationBenefitDto.setDistance(locationBenefit.getDistance());
                        return locationBenefitDto;
                    }).toList();
        }
        detailDto.setProjectLocationBenefitList(locationBenefitList);

        List<ProjectFaqDto> projectFaqList = new ArrayList<>();
        if (project.getProjectFaqs() != null) {
            projectFaqList = project.getProjectFaqs().stream().filter(Objects::nonNull)
                    .map(projectFaq -> {
                        ProjectFaqDto projectFaqDto = new ProjectFaqDto();
                        projectFaqDto.setProjectId(project.getId());
                        projectFaqDto.setQuestion(projectFaq.getFaqQuestion());
                        projectFaqDto.setAnswer(projectFaq.getFaqAnswer());
                        projectFaqDto.setId(projectFaq.getId());
                        return projectFaqDto;
                    }).toList();
        }
        detailDto.setProjectFaqList(projectFaqList);
        List<ProjectMobileBannerDto> projectMobileBannerDtoList = new ArrayList<>();
        if (project.getProjectMobileBanners() != null) {
            List<ProjectMobileBanner> projectMobileBanners = project.getProjectMobileBanners();
            projectMobileBannerDtoList = projectMobileBanners.stream().map(mobileBanner -> {
                ProjectMobileBannerDto projectMobileBannerDto = new ProjectMobileBannerDto();
                projectMobileBannerDto.setProjectId(project.getId());
                projectMobileBannerDto.setMobileImage(mobileBanner.getMobileImage());
                projectMobileBannerDto.setSlugURL(project.getSlugURL());
                projectMobileBannerDto.setMobileAltTag(mobileBanner.getMobileAltTag());
                projectMobileBannerDto.setProjectName(project.getProjectName());
                projectMobileBannerDto.setId(mobileBanner.getId());
                return projectMobileBannerDto;
            }).toList();
        }
        detailDto.setProjectMobileBannerDtoList(projectMobileBannerDtoList);
        List<ProjectDesktopBannerDto> projectDesktopBannerDtoList = new ArrayList<>();
        if (project.getProjectMobileBanners() != null) {
            List<ProjectDesktopBanner> projectDesktopBanners = project.getProjectDesktopBanners();
            projectDesktopBannerDtoList = projectDesktopBanners.stream().map(desktopBanner -> {
                ProjectDesktopBannerDto projectDesktopBannerDto = new ProjectDesktopBannerDto();
                projectDesktopBannerDto.setProjectId(project.getId());
                projectDesktopBannerDto.setDesktopImage(desktopBanner.getDesktopImage());
                projectDesktopBannerDto.setSlugURL(project.getSlugURL());
                projectDesktopBannerDto.setProjectName(project.getProjectName());
                projectDesktopBannerDto.setId(desktopBanner.getId());
                projectDesktopBannerDto.setDesktopAltTag(desktopBanner.getDesktopAltTag());
                return projectDesktopBannerDto;
            }).toList();
        }
        detailDto.setProjectDesktopBannerDtoList(projectDesktopBannerDtoList);
        // Calculate totalPrice from projectPrice if possible, or use from totalPrice field if exists
        if (project.getProjectPrice() != null && !project.getProjectPrice().isEmpty()) {
            try {
                String priceStr = project.getProjectPrice().replaceAll("[^0-9.Ee+-]", "");
                if (!priceStr.isEmpty()) {
                    detailDto.setTotalPrice(Double.parseDouble(priceStr));
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
    }

    public void mapCityToCityDto(City city, CityDto cityDto) {
        city.setName(cityDto.getCityName());
        city.setSlugUrl(cityDto.getSlugURL());
        city.setMetaTitle(cityDto.getMetaTitle());
        city.setMetaKeyWords(cityDto.getMetaKeywords());
        city.setMetaDescription(cityDto.getMetaDescription());
        city.setCityDisc(cityDto.getCityDescription());
    }

    public void mapCityDtoToCity(CityDto cityDto, City city) {
        cityDto.setId(city.getId());
        cityDto.setCityName(city.getName());
        cityDto.setSlugURL(city.getSlugUrl());
        cityDto.setMetaTitle(city.getMetaTitle());
        cityDto.setMetaKeywords(city.getMetaKeyWords());
        cityDto.setMetaDescription(city.getMetaDescription());
        cityDto.setCityDescription(city.getCityDisc());
    }

    public void mapShortProjectDetails(Project project, ProjectShortDetails projectShortDetails) {
        projectShortDetails.setId(project.getId());
        projectShortDetails.setProjectPrice(project.getProjectPrice());
        projectShortDetails.setProjectLocality(project.getProjectLocality());
        projectShortDetails.setProjectConfiguration(project.getProjectConfiguration());
        projectShortDetails.setProjectName(project.getProjectName());
        projectShortDetails.setProjectPrice(project.getProjectPrice());
        projectShortDetails.setStatus(project.isStatus());
        projectShortDetails.setSlugURL(project.getSlugURL());
        projectShortDetails.setProjectThumbnailImage(project.getProjectThumbnail());
        projectShortDetails.setProjectLogo(project.getProjectLogo());
        if (project.getProjectTypes() != null) {
            ProjectTypes projectTypes = project.getProjectTypes();
            projectShortDetails.setPropertyTypeName(projectTypes.getProjectTypeName());
        }
        if (project.getProjectStatus() != null) {
            ProjectStatus projectStatus = project.getProjectStatus();
            projectShortDetails.setProjectStatusName(projectStatus.getStatusName());
        }
        if (project.getCity() != null) {
            City city = project.getCity();
            projectShortDetails.setCityName(city.getName());
            projectShortDetails.setProjectAddress(project.getProjectLocality().concat(", ") + city.getName());
        }
        if (project.getBuilder() != null) {
            Builder builder = project.getBuilder();
            projectShortDetails.setBuilderName(builder.getBuilderName());
            projectShortDetails.setBuilderSlug(builder.getSlugUrl());
        }
        if (project.getProjectDesktopBanners() != null) {
            for (ProjectDesktopBanner banner : project.getProjectDesktopBanners()) {
                projectShortDetails.setProjectBannerImage(banner.getDesktopImage());
                break;
            }
        }
        projectShortDetails.setCreatedAt(project.getCreatedAt());
        projectShortDetails.setUpdatedAt(project.getUpdatedAt());
    }
}
