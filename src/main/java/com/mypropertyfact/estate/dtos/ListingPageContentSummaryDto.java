package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingPageContentSummaryDto {
    private int id;
    private String pageSlug;
    private String pageTitle;
    private String heading;
    private String metaTitle;
    private boolean isActive;
    private boolean hasContent;
}
