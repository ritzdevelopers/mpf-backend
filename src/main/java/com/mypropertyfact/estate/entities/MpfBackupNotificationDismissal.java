package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "mpf_backup_notification_dismissal",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mpf_backup_dismiss_user_run",
                columnNames = {"user_id", "backup_run_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MpfBackupNotificationDismissal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "backup_run_id", nullable = false)
    private Long backupRunId;

    @Column(name = "dismissed_at", nullable = false, updatable = false)
    private LocalDateTime dismissedAt;

    @PrePersist
    void onCreate() {
        if (dismissedAt == null) {
            dismissedAt = LocalDateTime.now();
        }
    }
}
