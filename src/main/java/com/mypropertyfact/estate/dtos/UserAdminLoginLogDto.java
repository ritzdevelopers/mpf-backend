package com.mypropertyfact.estate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminLoginLogDto {
    private Long id;
    private LocalDateTime loggedInAt;
    private String source;
    private String locationLabel;
    private String accountStatus;
    private String userAgent;
}
