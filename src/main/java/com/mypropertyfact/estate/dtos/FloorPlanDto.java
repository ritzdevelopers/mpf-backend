package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FloorPlanDto {
    private int projectId;
    private String pName;
    private String planType;
    private Double areaSqFt;
    private Double areaSqMt;
    private int floorId;

    public FloorPlanDto(String planType, Double areaSqFt, Double areaSqMt) {
        this.planType = planType;
        this.areaSqFt = areaSqFt;
        this.areaSqMt = areaSqMt;
    }
}
