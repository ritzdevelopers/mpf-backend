package com.mypropertyfact.estate.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "website_login_event",
        indexes = {
                @Index(name = "idx_website_login_at", columnList = "logged_in_at"),
                @Index(name = "idx_website_login_user", columnList = "user_id,logged_in_at"),
                @Index(name = "idx_website_login_phone", columnList = "phone")
        })
@Getter
@Setter
@NoArgsConstructor
public class WebsiteLoginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "logged_in_at", nullable = false, updatable = false)
    private LocalDateTime loggedInAt;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "user_category", length = 32)
    private String userCategory;

    @Column(name = "user_type", length = 64)
    private String userType;

    @Column(length = 255)
    private String roles;

    @Column
    private Boolean verified;

    @Column
    private Boolean enabled;

    @Column(name = "account_status", length = 32)
    private String accountStatus;

    @Column(name = "remote_addr", length = 64)
    private String remoteAddr;

    @Column(length = 64)
    private String country;

    @Column(length = 64)
    private String region;

    @Column(length = 64)
    private String city;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 255)
    private String org;

    @Column(name = "location_label", length = 255)
    private String locationLabel;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 64)
    private String source;

    @Column(name = "user_snapshot", columnDefinition = "TEXT")
    private String userSnapshot;
}
