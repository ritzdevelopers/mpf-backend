package com.mypropertyfact.estate.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPasswordResetSubmitRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 100)
    private String dashboardUsername;

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword;

    @NotBlank
    @Size(min = 8, max = 128)
    private String confirmPassword;
}
