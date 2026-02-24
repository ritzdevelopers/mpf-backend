package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMobileBannerDto {
    private long id;
    private int projectId;
    private String projectName;
    private String slugURL;
    private String mobileAltTag;
    private String mobileImage;

    public ProjectMobileBannerDto(String mobileImage, String mobileAltTag) {
        this.mobileImage = mobileImage;
        this.mobileAltTag = mobileAltTag;
    }
}
