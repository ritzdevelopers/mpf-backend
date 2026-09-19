package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingPageFaqGroupDto {
    private String pageSlug;
    private String pageTitle;
    private int noOfFaqs;
    private List<ListingPageFaqItemDto> faqs;
}
