package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OTP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "expires_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;

    /** Nullable only on legacy rows until migrated; always set on new OTPs. */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 32)
    private OtpPurpose purpose;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createdAt = now;
        expiresAt = new Date(now.getTime() + 5 * 60 * 1000); // 5 minutes
        if (purpose == null) {
            purpose = OtpPurpose.MAGIC_LINK;
        }
    }
}

