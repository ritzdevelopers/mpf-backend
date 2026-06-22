package com.mypropertyfact.estate.dtos;

import lombok.Data;

import java.util.List;

@Data
public class CityDto {
    private int id;
    private int stateId;
    private int countryId;
    private int districtId;
    private String metaTitle;
    private String metaKeywords;
    private String metaDescription;
    private String cityName;
    private String stateName;
    private String countryName;
    private String cityDescription;
    private String cityImage;
    private String monumentName;
    private String monumentImage;
    private String cityHighlights;
    private Boolean isActive;
    private String slugURL;
    private List<ProjectDetailDto> projectList;
    private List<LocalityDto> localityList;
}
