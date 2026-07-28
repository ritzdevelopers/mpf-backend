package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListingPageFaqDto {
    private int id;
    private String pageSlug;
    private String pageTitle;
    private String question;
    private String answer;
    private int sortOrder;
}
