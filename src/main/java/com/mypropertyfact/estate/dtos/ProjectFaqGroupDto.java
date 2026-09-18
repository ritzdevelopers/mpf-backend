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
public class ProjectFaqGroupDto {
    private int projectId;
    private String projectName;
    private int noOfFaqs;
    private List<ProjectFaqItemDto> projectFaq;
}
