package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListingPageFaqBulkDto {
    /** One or more FAQ rows; each may target a different page slug. */
    private List<ListingPageFaqDto> faqs;
}
