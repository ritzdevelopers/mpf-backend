package com.mypropertyfact.estate.dtos;

import com.mypropertyfact.estate.entities.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CreateUserBySuperAdminResponse {
    private User user;
    /** Plaintext password (same as request); shown once in admin UI. */
    private String password;
    /** Plaintext enquiries PIN when set; null otherwise. */
    private String enquiryAccessPin;
    private List<String> roleNames;
}
