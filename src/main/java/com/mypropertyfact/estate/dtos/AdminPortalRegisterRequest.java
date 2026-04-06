package com.mypropertyfact.estate.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AdminPortalRegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** Required when the account includes the Admin role (dashboard login at /admin). */
    private String dashboardUsername;

    /**
     * Optional extra roles (ADMIN) from /auth/admin-register-meta.
     * If null or empty, the account is created with the USER role only (default).
     */
    private List<Integer> roleIds;

    /** Required only when {@code app.admin.registration-pin} is set. */
    private String registrationPin;
}
