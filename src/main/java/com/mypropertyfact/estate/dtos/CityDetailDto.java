package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityDetailDto {
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
    private String slugURL;
    private List<ProjectShortDetails> projectList;
    private List<LocalityShortDto> localities;
}
