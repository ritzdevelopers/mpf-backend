package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuilderDto {
    private int id;
    private String metaTitle;
    private String metaKeywords;
    private String metaDescription;
    private String builderName;
    private String builderDescription;
    private String slugURL;
    private List<ProjectShortDetails> projectList;

    public BuilderDto(int id, String builderName, String builderDescription, String slugURL) {
        this.id = id;
        this.builderName = builderName;
        this.builderDescription = builderDescription;
        this.slugURL = slugURL;
    }
}
