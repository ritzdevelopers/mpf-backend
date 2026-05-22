package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.backup.MpfBackupKind;
import com.mypropertyfact.estate.backup.MpfBackupRunStatus;
import com.mypropertyfact.estate.entities.MpfBackupRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MpfBackupRunRepository extends JpaRepository<MpfBackupRun, Long> {

    Optional<MpfBackupRun> findFirstByStatusOrderByCompletedAtDesc(MpfBackupRunStatus status);

    Optional<MpfBackupRun> findFirstByStatusAndBackupKindOrderByCompletedAtDesc(
            MpfBackupRunStatus status, MpfBackupKind backupKind);

    boolean existsByStatusAndBackupKind(MpfBackupRunStatus status, MpfBackupKind backupKind);

    Page<MpfBackupRun> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<MpfBackupRun> findByStatusAndCreatedAtBefore(MpfBackupRunStatus status, LocalDateTime cutoff);

    boolean existsByStatus(MpfBackupRunStatus status);

    List<MpfBackupRun> findByStatus(MpfBackupRunStatus status);

    List<MpfBackupRun> findByStatusAndBackupKind(MpfBackupRunStatus status, MpfBackupKind backupKind);
}
