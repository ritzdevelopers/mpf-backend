package com.mypropertyfact.estate.dtos;

import lombok.Data;

import java.util.List;

@Data
public class WebStoryCategoryDto {
    private int id;
    private String categoryName;
    private String categoryDescription;
    private String metaDescription;
    private String metaKeywords;
    private List<WebStoryDto> webStories;
    private String storyCategoryImage;
}
