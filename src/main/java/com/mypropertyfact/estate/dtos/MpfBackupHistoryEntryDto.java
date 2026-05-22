package com.mypropertyfact.estate.dtos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MpfBackupHistoryEntryDto {
    Long id;
    String status;
    String triggerType;
    String backupKind;
    String createdAt;
    String completedAt;
    Long fileSizeBytes;
    boolean hadChangesSincePrevious;
    String errorMessage;
}
