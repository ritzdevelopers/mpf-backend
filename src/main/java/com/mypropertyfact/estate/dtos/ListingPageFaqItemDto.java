package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListingPageFaqItemDto {
    private int id;
    private String question;
    private String answer;
    private int sortOrder;
}
