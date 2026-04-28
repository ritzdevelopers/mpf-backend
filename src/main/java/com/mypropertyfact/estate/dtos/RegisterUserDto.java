package com.mypropertyfact.estate.dtos;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class RegisterUserDto {
    private String email;

    private String password;

    private String fullName;

    /** E.164 preferred (e.g. +919876543210). Used by mobile sign-up when collected. */
    private String phone;

    private String role; // Optional: will default to "ROLE_USER" if not provided
}
