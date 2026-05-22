package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.MpfBackupNotificationDismissal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MpfBackupNotificationDismissalRepository
        extends JpaRepository<MpfBackupNotificationDismissal, Long> {

    boolean existsByUserIdAndBackupRunId(Integer userId, Long backupRunId);
}
