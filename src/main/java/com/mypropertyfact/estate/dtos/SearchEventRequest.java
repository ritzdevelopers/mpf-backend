package com.mypropertyfact.estate.dtos;

import lombok.Data;

@Data
public class SearchEventRequest {
    /** Free-text or selected search label. */
    private String query;
    /** property | blog | keyword */
    private String searchType;
    private String targetRef;
    private String targetLabel;
    private Integer resultCount;
    private String sourcePath;
    private String clientSessionId;
}
