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
public class WebsiteLoginEventDto {
    private Long id;
    private LocalDateTime loggedInAt;
    private Integer userId;
    private String fullName;
    private String phone;
    private String email;
    private String userCategory;
    private String userType;
    private String roles;
    private Boolean verified;
    private Boolean enabled;
    private String accountStatus;
    private String ip;
    private String country;
    private String region;
    private String city;
    private Double latitude;
    private Double longitude;
    private String org;
    private String locationLabel;
    private String userAgent;
    private String source;
    private String userSnapshot;
    private boolean ipRevealed;
}
