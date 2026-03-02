package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeBannerDto {
    private Integer id;
    private String imageId;
    private String imageName;
    private String imageAlt;
    private String imageHeight;
    private String imageWidth;
    private String deviceType;
    private String bannerLink;
}
