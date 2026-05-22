package com.mypropertyfact.estate.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordOtpCompleteRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String otp;

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword;
}
