package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminLogsResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String userCategory;
    private Boolean enabled;
    private Boolean verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> roles;
    private long viewedCount;
    private long shortlistedCount;
    private long searchCount;
    private long loginCount;
    private List<UserActivityItemDto> recentActivity;
    private List<UserAdminLoginLogDto> recentLogins;
}
