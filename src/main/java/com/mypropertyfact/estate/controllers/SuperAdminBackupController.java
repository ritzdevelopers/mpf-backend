package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.backup.BackupInProgressException;
import com.mypropertyfact.estate.backup.MpfBackupConstants;
import com.mypropertyfact.estate.backup.MpfBackupKind;
import com.mypropertyfact.estate.backup.MpfBackupRunStatus;
import com.mypropertyfact.estate.dtos.MpfBackupHistoryEntryDto;
import com.mypropertyfact.estate.dtos.MpfBackupHistoryPageResponse;
import com.mypropertyfact.estate.dtos.MpfBackupStatusResponse;
import com.mypropertyfact.estate.entities.MpfBackupRun;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.MpfBackupRunRepository;
import com.mypropertyfact.estate.services.MpfBackupNotificationService;
import com.mypropertyfact.estate.services.MpfBackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/super/backup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuperAdminBackupController {

    private final MpfBackupService backupService;
    private final MpfBackupNotificationService notificationService;
    private final MpfBackupRunRepository backupRunRepository;

    @GetMapping("/status")
    public ResponseEntity<MpfBackupStatusResponse> status(
            Authentication authentication,
            @RequestParam(required = false) String preview) {
        User user = requireUser(authentication);
        String firstName = firstNameFrom(user);
        return ResponseEntity.ok(
                notificationService.buildStatus(user.getId(), firstName, preview));
    }

    /** Fast: Excel files only (admin text/data). */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runExcelBackup(Authentication authentication) {
        requireUser(authentication);
        MpfBackupRun run = backupService.startManualExcelBackup();
        return ResponseEntity.accepted()
                .body(Map.of(
                        "backupRunId", run.getId(),
                        "backupKind", MpfBackupKind.EXCEL.name(),
                        "status", run.getStatus().name()));
    }

    /** Slow: all image upload folders in one ZIP. */
    @PostMapping("/run/media")
    public ResponseEntity<Map<String, Object>> runMediaBackup(Authentication authentication) {
        requireUser(authentication);
        MpfBackupRun run = backupService.startManualMediaBackup();
        return ResponseEntity.accepted()
                .body(Map.of(
                        "backupRunId", run.getId(),
                        "backupKind", MpfBackupKind.MEDIA.name(),
                        "status", run.getStatus().name()));
    }

    /** Stop the current Excel export (and any other in-progress runs if kind omitted). */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelExcelBackup(Authentication authentication) {
        requireUser(authentication);
        int cancelled = backupService.cancelInProgress(MpfBackupKind.EXCEL);
        return ResponseEntity.ok(Map.of("cancelled", cancelled, "backupKind", MpfBackupKind.EXCEL.name()));
    }

    @PostMapping("/cancel/media")
    public ResponseEntity<Map<String, Object>> cancelMediaBackup(Authentication authentication) {
        requireUser(authentication);
        int cancelled = backupService.cancelInProgress(MpfBackupKind.MEDIA);
        return ResponseEntity.ok(Map.of("cancelled", cancelled, "backupKind", MpfBackupKind.MEDIA.name()));
    }

    @PostMapping("/cancel/all")
    public ResponseEntity<Map<String, Object>> cancelAllBackups(Authentication authentication) {
        requireUser(authentication);
        int cancelled = backupService.cancelAllInProgress();
        return ResponseEntity.ok(Map.of("cancelled", cancelled));
    }

    @ExceptionHandler(BackupInProgressException.class)
    public ResponseEntity<Map<String, Object>> backupInProgress(BackupInProgressException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "backup_in_progress",
                        "message", ex.getMessage(),
                        "activeRunId", ex.getActiveRunId(),
                        "backupKind", ex.getKind().name()));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(
            Authentication authentication,
            @PathVariable Long id) throws Exception {
        requireUser(authentication);
        if (id == null || id <= 0 || id == MpfBackupConstants.PREVIEW_RUN_ID) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_backup_id"));
        }
        MpfBackupRun run = backupService.findRun(id).orElse(null);
        if (run == null || run.getStatus() != MpfBackupRunStatus.READY) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "backup_not_ready"));
        }
        Path file = backupService.resolveBackupFile(run).orElse(null);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "backup_file_missing"));
        }
        InputStream in = Files.newInputStream(file);
        String filename = file.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(Files.size(file))
                .body(new InputStreamResource(in));
    }

    @PostMapping("/dismiss/{id}")
    public ResponseEntity<Void> dismiss(
            Authentication authentication,
            @PathVariable Long id) {
        User user = requireUser(authentication);
        notificationService.dismiss(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<MpfBackupHistoryPageResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        Page<MpfBackupRun> result = backupRunRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        MpfBackupHistoryPageResponse body = MpfBackupHistoryPageResponse.builder()
                .content(result.getContent().stream().map(this::toHistoryDto).toList())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .number(result.getNumber())
                .size(result.getSize())
                .build();
        return ResponseEntity.ok(body);
    }

    private MpfBackupHistoryEntryDto toHistoryDto(MpfBackupRun run) {
        return MpfBackupHistoryEntryDto.builder()
                .id(run.getId())
                .status(run.getStatus() != null ? run.getStatus().name() : null)
                .triggerType(run.getTriggerType() != null ? run.getTriggerType().name() : null)
                .backupKind(run.getBackupKind() != null ? run.getBackupKind().name() : null)
                .createdAt(run.getCreatedAt() != null ? run.getCreatedAt().toString() : null)
                .completedAt(run.getCompletedAt() != null ? run.getCompletedAt().toString() : null)
                .fileSizeBytes(run.getFileSizeBytes())
                .hadChangesSincePrevious(run.isHadChangesSincePrevious())
                .errorMessage(run.getErrorMessage())
                .build();
    }

    private static User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        }
        return user;
    }

    private static String firstNameFrom(User user) {
        String name = user.getFullName();
        if (name == null || name.isBlank()) {
            name = user.getDashboardUsername();
        }
        if (name == null || name.isBlank()) {
            return "Super Admin";
        }
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }
}
