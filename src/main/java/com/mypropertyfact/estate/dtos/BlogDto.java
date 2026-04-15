package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BlogDto {
    private int id;
    private String blogTitle;
    private String blogKeywords;
    private String blogMetaDescription;
    private String blogDescription;
    private String slugUrl;
    private String blogImage;
    private String authorName;
    private String blogCategory;
    private int status;
    private int categoryId;
    private int cityId;
    private String cityName;
    private LocalDateTime createdAt;
}
