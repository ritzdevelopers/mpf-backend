package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.backup.MpfBackupConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MpfBackupScheduler {

    private final MpfBackupScheduleService scheduleService;
    private final MpfBackupService backupService;

    @Scheduled(cron = MpfBackupConstants.CRON_WEEKLY_MONDAY_2PM)
    public void runWeeklyBackup() {
        LocalDateTime now = LocalDateTime.now();
        if (!scheduleService.isEligibleForScheduledRun(now)) {
            log.debug("MPF weekly backup skipped — before first eligible time {}", 
                    scheduleService.getOrInitFirstEligibleBackupAt());
            return;
        }
        if (backupService.isBackupInProgress()) {
            log.warn("MPF weekly backup skipped — another backup in progress");
            return;
        }
        try {
            backupService.startScheduledBackupIfNeeded().ifPresent(run ->
                    log.info("MPF weekly backup finished with status {}", run.getStatus()));
        } catch (Exception e) {
            log.error("MPF weekly backup scheduler error", e);
        }
    }
}
