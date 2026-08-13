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
public class IpTrackIpPageResponse {
    private List<IpTrackIpSummaryDto> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
    private boolean ipRevealActive;
}
