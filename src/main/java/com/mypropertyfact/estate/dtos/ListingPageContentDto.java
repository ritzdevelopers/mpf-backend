package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListingPageContentDto {
    private int id;
    private String pageSlug;
    private String pageTitle;
    private String heading;
    private String intro;
    private String content;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private boolean isActive = true;
}
