package com.mypropertyfact.estate.dtos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PendingPermissionsCountResponse {
    int adminAccessPending;
    int passwordChangePending;
    int totalPending;
}
