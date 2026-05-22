package com.mypropertyfact.estate.dtos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MpfBackupStatusResponse {
    String bannerState;
    Long backupRunId;
    String firstName;
    String createdAt;
    /** ISO timestamp when the current run started (for client-side elapsed timer). */
    String startedAt;
    Long fileSizeBytes;
    /** EXCEL or MEDIA */
    String backupKind;
    boolean devPreview;
    String message;
}
