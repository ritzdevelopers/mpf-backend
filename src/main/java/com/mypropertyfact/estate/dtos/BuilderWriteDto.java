package com.mypropertyfact.estate.dtos;

import lombok.Data;

/**
 * Payload for {@code POST /api/v1/builder/add-update}. Keeps request body free of
 * read-only / relation fields (timestamps, projects, grid {@code index}) that can
 * cause 500s when bound to {@link com.mypropertyfact.estate.entities.Builder}.
 */
@Data
public class BuilderWriteDto {
    private int id;
    private String builderName;
    private String builderDesc;
    private String metaTitle;
    private String metaKeyword;
    private String metaDesc;
}
