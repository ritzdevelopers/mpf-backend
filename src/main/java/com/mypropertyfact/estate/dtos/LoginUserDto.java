package com.mypropertyfact.estate.dtos;

import lombok.Data;

@Data
public class LoginUserDto {
    private String email;

    private String password;

    /** Required for users with Super Admin or Admin role when signing in at /admin. */
    private String dashboardUsername;
}
