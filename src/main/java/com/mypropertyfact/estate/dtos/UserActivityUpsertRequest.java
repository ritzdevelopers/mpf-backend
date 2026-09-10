package com.mypropertyfact.estate.dtos;

import lombok.Data;

@Data
public class UserActivityUpsertRequest {
    private String type;
    private String action;
    private String entityType;
    private String entityId;
    private String entitySlug;
    private String entityLabel;
    private String href;
}
