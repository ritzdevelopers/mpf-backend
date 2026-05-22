package com.mypropertyfact.estate.entities;

import com.mypropertyfact.estate.backup.MpfBackupKind;
import com.mypropertyfact.estate.backup.MpfBackupRunStatus;
import com.mypropertyfact.estate.backup.MpfBackupTrigger;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mpf_backup_run", indexes = {
        @Index(name = "idx_mpf_backup_run_created", columnList = "created_at"),
        @Index(name = "idx_mpf_backup_run_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class MpfBackupRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MpfBackupRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 16)
    private MpfBackupTrigger triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "backup_kind", nullable = false, length = 16)
    private MpfBackupKind backupKind = MpfBackupKind.EXCEL;

    @Column(name = "file_path", length = 1024)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "had_changes_since_previous", nullable = false)
    private boolean hadChangesSincePrevious;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
