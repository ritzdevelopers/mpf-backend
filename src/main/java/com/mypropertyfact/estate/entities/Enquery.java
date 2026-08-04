package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "enquiries")
public class Enquery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String email;
    private String phone;
    private String message;
    private String enquiryFrom;
    private String projectLink;
    private String pageName;
    private String status;
    private Long propertyId; // Property/Project ID for lead management
    @Column(columnDefinition = "JSON")
    private String metadataJson;
    private String whatsapp;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
