package com.mypropertyfact.estate.dtos;

import com.mypropertyfact.estate.entities.User;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PendingPermissionsResponse {
    List<User> adminAccessRequests;
    List<PasswordResetPendingRowDto> passwordChangeRequests;
}
