package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationBenefitDto {
    private int id;
    private String distance;
    private String benefitName;
    private String image;
    private String projectName;
    private int projectId;
    private String slugUrl;
    public LocationBenefitDto(int id, int projectId, String distance, String bName, String image, String pName){
        this.id = id;
        this.projectId = projectId;
        this.distance = distance;
        this.benefitName = bName;
        this.image = image;
        this.projectName = pName;
    }
    public LocationBenefitDto(String benefitName, String distance) {
        this.benefitName = benefitName;
        this.distance = distance;
    }
}
