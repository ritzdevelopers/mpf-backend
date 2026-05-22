package com.mypropertyfact.estate.backup;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class MpfBackupConstants {

    private MpfBackupConstants() {}

    /** Enable dev-only banner preview when {@code dev} profile is active. */
    public static final boolean LOCAL_DEV_BANNER_PREVIEW = true;

    public static final String CRON_WEEKLY_MONDAY_2PM = "0 0 14 * * MON";

    public static final int RETENTION_WEEKS = 8;

    public static final long PREVIEW_RUN_ID = -1L;

    public static final Path STORAGE_DIR =
            Paths.get(System.getProperty("user.dir"), "data", "mpf-backups").toAbsolutePath().normalize();

    public static final String MYSQLDUMP_COMMAND = "mysqldump";

    /** Excel backup stuck longer than this is auto-released. */
    public static final int STUCK_EXCEL_MINUTES = 20;

    /** Media backup stuck longer than this is auto-released. */
    public static final int STUCK_MEDIA_MINUTES = 180;
}
