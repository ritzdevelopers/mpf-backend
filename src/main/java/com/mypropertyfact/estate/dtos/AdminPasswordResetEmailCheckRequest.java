package com.mypropertyfact.estate.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminPasswordResetEmailCheckRequest {

    @NotBlank
    @Email
    private String email;
}
