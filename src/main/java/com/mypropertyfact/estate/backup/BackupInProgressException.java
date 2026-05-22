package com.mypropertyfact.estate.backup;

import lombok.Getter;

@Getter
public class BackupInProgressException extends IllegalStateException {

    private final Long activeRunId;
    private final MpfBackupKind kind;

    public BackupInProgressException(Long activeRunId, MpfBackupKind kind) {
        super("A " + kind.name().toLowerCase() + " backup is already in progress (run #" + activeRunId + ").");
        this.activeRunId = activeRunId;
        this.kind = kind;
    }
}
