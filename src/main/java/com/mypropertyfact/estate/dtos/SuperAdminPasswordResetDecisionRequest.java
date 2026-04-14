package com.mypropertyfact.estate.dtos;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Approve with optional override: if {@code editedPassword} is blank, the admin's proposed
 * password (already stored hashed) is applied; otherwise this new plaintext is encoded and applied.
 */
@Data
public class SuperAdminPasswordResetDecisionRequest {

    @Size(min = 8, max = 128)
    private String editedPassword;
}
