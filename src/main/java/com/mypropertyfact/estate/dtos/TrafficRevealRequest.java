package com.mypropertyfact.estate.dtos;

import lombok.Data;

@Data
public class TrafficRevealRequest {
    /** Must match configured traffic insights PIN (default 2026). */
    private String pin;
}
