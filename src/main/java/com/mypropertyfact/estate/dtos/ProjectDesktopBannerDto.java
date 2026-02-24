package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDesktopBannerDto {
    private long id;
    private int projectId;
    private String projectName;
    private String slugURL;
    private String desktopImage;
    private String desktopAltTag;

    public ProjectDesktopBannerDto(String desktopImage, String desktopAltTag) {
        this.desktopImage = desktopImage;
        this.desktopAltTag = desktopAltTag;
    }
}
