package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.MpfBackupNotificationDismissal;
import com.mypropertyfact.estate.entities.MpfBackupRun;
import com.mypropertyfact.estate.repositories.MpfBackupNotificationDismissalRepository;
import com.mypropertyfact.estate.repositories.MpfBackupRunRepository;
import com.mypropertyfact.estate.backup.MpfBackupConstants;
import com.mypropertyfact.estate.backup.MpfBackupKind;
import com.mypropertyfact.estate.backup.MpfBackupRunStatus;
import com.mypropertyfact.estate.dtos.MpfBackupStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MpfBackupNotificationService {

    private final MpfBackupRunRepository backupRunRepository;
    private final MpfBackupNotificationDismissalRepository dismissalRepository;
    private final MpfDataChangeDetectorService changeDetectorService;
    private final MpfBackupService backupService;
    private final Environment environment;

    public MpfBackupStatusResponse buildStatus(Integer userId, String firstName, String preview) {
        try {
            return buildStatusInternal(userId, firstName, preview);
        } catch (Exception e) {
            if (devPreviewEnabled()) {
                if ("ready".equalsIgnoreCase(preview)) {
                    return MpfBackupStatusResponse.builder()
                            .bannerState("ready")
                            .backupRunId(MpfBackupConstants.PREVIEW_RUN_ID)
                            .backupKind(MpfBackupKind.EXCEL.name())
                            .firstName(firstName)
                            .devPreview(true)
                            .message("Dev preview — Excel data ready.")
                            .build();
                }
                return idleResponse(MpfBackupConstants.PREVIEW_RUN_ID, firstName, true);
            }
            throw e;
        }
    }

    private MpfBackupStatusResponse buildStatusInternal(Integer userId, String firstName, String preview) {
        backupService.releaseStuckInProgressRuns();
        if (backupService.isBackupInProgress()) {
            Optional<MpfBackupRun> inProgress = backupRunRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1))
                    .stream()
                    .filter(r -> r.getStatus() == MpfBackupRunStatus.IN_PROGRESS)
                    .findFirst();
            MpfBackupRun run = inProgress.orElse(null);
            String kind = run != null && run.getBackupKind() != null ? run.getBackupKind().name() : "EXCEL";
            String msg =
                    run != null && run.getBackupKind() == MpfBackupKind.MEDIA
                            ? "Packaging images into ZIP…"
                            : "Exporting admin data to Excel…";
            return MpfBackupStatusResponse.builder()
                    .bannerState("inProgress")
                    .backupRunId(run != null ? run.getId() : null)
                    .backupKind(kind)
                    .firstName(firstName)
                    .startedAt(run != null && run.getCreatedAt() != null ? run.getCreatedAt().toString() : null)
                    .devPreview(false)
                    .message(msg)
                    .build();
        }

        Optional<MpfBackupRun> latestExcelReady = backupRunRepository
                .findFirstByStatusAndBackupKindOrderByCompletedAtDesc(
                        MpfBackupRunStatus.READY, MpfBackupKind.EXCEL);
        if (latestExcelReady.isPresent()) {
            MpfBackupRun run = latestExcelReady.get();
            if (!isDismissed(userId, run.getId())) {
                return MpfBackupStatusResponse.builder()
                        .bannerState("ready")
                        .backupRunId(run.getId())
                        .backupKind(MpfBackupKind.EXCEL.name())
                        .firstName(firstName)
                        .createdAt(run.getCompletedAt() != null ? run.getCompletedAt().toString() : null)
                        .fileSizeBytes(run.getFileSizeBytes())
                        .devPreview(false)
                        .message("Excel data backup is ready to download.")
                        .build();
            }
        }

        Optional<MpfBackupRun> latestSkipped = findLatestSkipped();
        LocalDateTime lastReadyAt = backupService.lastSuccessfulExcelBackupCompletedAt().orElse(null);
        boolean noChanges = !changeDetectorService.hasDataChangesSince(lastReadyAt);

        if (latestSkipped.isPresent() && noChanges) {
            MpfBackupRun run = latestSkipped.get();
            if (!isDismissed(userId, run.getId())) {
                return idleResponse(run.getId(), firstName, false);
            }
        }

        if (noChanges) {
            Long runId = latestSkipped.map(MpfBackupRun::getId).orElse(null);
            if (runId == null || !isDismissed(userId, runId)) {
                return idleResponse(runId, firstName, false);
            }
        }

        Optional<MpfBackupRun> latestFailed = findLatestFailed();
        if (latestFailed.isPresent() && !isDismissed(userId, latestFailed.get().getId())) {
            MpfBackupRun run = latestFailed.get();
            return MpfBackupStatusResponse.builder()
                    .bannerState("failed")
                    .backupRunId(run.getId())
                    .firstName(firstName)
                    .devPreview(false)
                    .message(run.getErrorMessage() != null ? run.getErrorMessage() : "Backup failed.")
                    .build();
        }

        if (devPreviewEnabled()) {
            if ("ready".equalsIgnoreCase(preview)) {
                return MpfBackupStatusResponse.builder()
                        .bannerState("ready")
                        .backupRunId(MpfBackupConstants.PREVIEW_RUN_ID)
                        .firstName(firstName)
                        .devPreview(true)
                        .message("Dev preview — backup ready message.")
                        .build();
            }
            return idleResponse(MpfBackupConstants.PREVIEW_RUN_ID, firstName, true);
        }

        return MpfBackupStatusResponse.builder()
                .bannerState("none")
                .firstName(firstName)
                .devPreview(false)
                .build();
    }

    private MpfBackupStatusResponse idleResponse(Long runId, String firstName, boolean devPreview) {
        return MpfBackupStatusResponse.builder()
                .bannerState("idleNoChanges")
                .backupRunId(runId)
                .backupKind(MpfBackupKind.EXCEL.name())
                .firstName(firstName)
                .devPreview(devPreview)
                .message("Nothing new on MPF since last week. Export current admin data to Excel?")
                .build();
    }

    private boolean devPreviewEnabled() {
        return MpfBackupConstants.LOCAL_DEV_BANNER_PREVIEW
                && Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private boolean isDismissed(Integer userId, Long runId) {
        if (userId == null || runId == null) {
            return false;
        }
        if (runId == MpfBackupConstants.PREVIEW_RUN_ID) {
            return false;
        }
        return dismissalRepository.existsByUserIdAndBackupRunId(userId, runId);
    }

    private Optional<MpfBackupRun> findLatestSkipped() {
        return backupRunRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)).stream()
                .filter(r -> r.getStatus() == MpfBackupRunStatus.SKIPPED_NO_CHANGES)
                .findFirst();
    }

    private Optional<MpfBackupRun> findLatestFailed() {
        return backupRunRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)).stream()
                .filter(r -> r.getStatus() == MpfBackupRunStatus.FAILED)
                .findFirst();
    }

    @Transactional
    public void dismiss(Integer userId, Long backupRunId) {
        if (userId == null || backupRunId == null || backupRunId == MpfBackupConstants.PREVIEW_RUN_ID) {
            return;
        }
        if (dismissalRepository.existsByUserIdAndBackupRunId(userId, backupRunId)) {
            return;
        }
        MpfBackupNotificationDismissal row = new MpfBackupNotificationDismissal();
        row.setUserId(userId);
        row.setBackupRunId(backupRunId);
        dismissalRepository.save(row);
    }
}
