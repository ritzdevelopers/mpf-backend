package com.mypropertyfact.estate.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterVerifyOtpRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String otp;
}
