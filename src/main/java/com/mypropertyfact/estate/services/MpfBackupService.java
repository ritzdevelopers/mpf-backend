package com.mypropertyfact.estate.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypropertyfact.estate.backup.MpfBackupConstants;
import com.mypropertyfact.estate.backup.MpfBackupKind;
import com.mypropertyfact.estate.backup.MpfBackupRunStatus;
import com.mypropertyfact.estate.backup.MpfBackupTrigger;
import com.mypropertyfact.estate.entities.MpfBackupRun;
import com.mypropertyfact.estate.repositories.MpfBackupRunRepository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class MpfBackupService {

    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss");

    private final MpfBackupRunRepository backupRunRepository;
    private final MpfDataChangeDetectorService changeDetectorService;
    private final MpfBackupExcelExportService excelExportService;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final Executor mpfBackupExecutor;

    private final ConcurrentHashMap<Long, AtomicBoolean> cancelSignals = new ConcurrentHashMap<>();

    @Value("${upload_dir}")
    private String uploadDir;

    @Value("${uploads_path}")
    private String uploadsPath;

    @Value("${upload_amenity_path}")
    private String uploadAmenityPath;

    @Value("${upload_icon_path}")
    private String uploadIconPath;

    @Value("${upload.dir:}")
    private String uploadDotDir;

    public MpfBackupService(
            MpfBackupRunRepository backupRunRepository,
            MpfDataChangeDetectorService changeDetectorService,
            MpfBackupExcelExportService excelExportService,
            DataSource dataSource,
            ObjectMapper objectMapper,
            @Qualifier("mpfBackupExecutor") Executor mpfBackupExecutor) {
        this.backupRunRepository = backupRunRepository;
        this.changeDetectorService = changeDetectorService;
        this.excelExportService = excelExportService;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.mpfBackupExecutor = mpfBackupExecutor;
    }

    public boolean isBackupInProgress() {
        return backupRunRepository.existsByStatus(MpfBackupRunStatus.IN_PROGRESS);
    }

    @Transactional
    public MpfBackupRun startManualExcelBackup() {
        return startBackupRun(MpfBackupTrigger.MANUAL, true, MpfBackupKind.EXCEL);
    }

    @Transactional
    public MpfBackupRun startManualMediaBackup() {
        return startBackupRun(MpfBackupTrigger.MANUAL, true, MpfBackupKind.MEDIA);
    }

    @Transactional
    public Optional<MpfBackupRun> startScheduledBackupIfNeeded() {
        LocalDateTime lastReady = lastSuccessfulExcelBackupCompletedAt().orElse(null);
        boolean changes = changeDetectorService.hasDataChangesSince(lastReady);
        if (!changes) {
            MpfBackupRun skipped = new MpfBackupRun();
            skipped.setStatus(MpfBackupRunStatus.SKIPPED_NO_CHANGES);
            skipped.setTriggerType(MpfBackupTrigger.SCHEDULED);
            skipped.setBackupKind(MpfBackupKind.EXCEL);
            skipped.setHadChangesSincePrevious(false);
            skipped.setCompletedAt(LocalDateTime.now());
            return Optional.of(backupRunRepository.save(skipped));
        }
        return Optional.of(startBackupRun(MpfBackupTrigger.SCHEDULED, true, MpfBackupKind.EXCEL));
    }

    @Transactional
    public int cancelAllInProgress() {
        int n = 0;
        for (MpfBackupRun run : backupRunRepository.findByStatus(MpfBackupRunStatus.IN_PROGRESS)) {
            markCancelled(run, "Cancelled by super admin");
            n++;
        }
        return n;
    }

    @Transactional
    public int cancelInProgress(MpfBackupKind kind) {
        MpfBackupKind safeKind = kind != null ? kind : MpfBackupKind.EXCEL;
        int n = 0;
        for (MpfBackupRun run :
                backupRunRepository.findByStatusAndBackupKind(MpfBackupRunStatus.IN_PROGRESS, safeKind)) {
            markCancelled(run, "Cancelled by super admin");
            n++;
        }
        return n;
    }

    @Transactional
    public void releaseStuckInProgressRuns() {
        LocalDateTime now = LocalDateTime.now();
        for (MpfBackupRun run : backupRunRepository.findByStatus(MpfBackupRunStatus.IN_PROGRESS)) {
            int maxMinutes =
                    run.getBackupKind() == MpfBackupKind.MEDIA
                            ? MpfBackupConstants.STUCK_MEDIA_MINUTES
                            : MpfBackupConstants.STUCK_EXCEL_MINUTES;
            if (run.getCreatedAt() != null && run.getCreatedAt().isBefore(now.minusMinutes(maxMinutes))) {
                markCancelled(run, "Timed out (stuck in progress)");
                log.warn("Released stuck backup run {}", run.getId());
            }
        }
    }

    @Transactional
    public MpfBackupRun startBackupRun(
            MpfBackupTrigger trigger, boolean forceDespiteNoChanges, MpfBackupKind kind) {
        MpfBackupKind safeKind = kind != null ? kind : MpfBackupKind.EXCEL;
        releaseStuckInProgressRuns();
        Optional<MpfBackupRun> active = backupRunRepository
                .findByStatusAndBackupKind(MpfBackupRunStatus.IN_PROGRESS, safeKind)
                .stream()
                .findFirst();
        if (active.isEmpty()) {
            active = backupRunRepository.findByStatus(MpfBackupRunStatus.IN_PROGRESS).stream()
                    .filter(r -> r.getBackupKind() == safeKind)
                    .findFirst();
        }
        if (active.isPresent()) {
            throw new com.mypropertyfact.estate.backup.BackupInProgressException(
                    active.get().getId(), safeKind);
        }
        LocalDateTime lastReady = lastSuccessfulBackupCompletedAt(safeKind).orElse(null);
        boolean changes = changeDetectorService.hasDataChangesSince(lastReady);
        if (!forceDespiteNoChanges && !changes && safeKind == MpfBackupKind.EXCEL) {
            MpfBackupRun skipped = new MpfBackupRun();
            skipped.setStatus(MpfBackupRunStatus.SKIPPED_NO_CHANGES);
            skipped.setTriggerType(trigger);
            skipped.setBackupKind(safeKind);
            skipped.setHadChangesSincePrevious(false);
            skipped.setCompletedAt(LocalDateTime.now());
            return backupRunRepository.save(skipped);
        }
        MpfBackupRun run = new MpfBackupRun();
        run.setStatus(MpfBackupRunStatus.IN_PROGRESS);
        run.setTriggerType(trigger);
        run.setBackupKind(safeKind);
        run.setHadChangesSincePrevious(changes || safeKind == MpfBackupKind.MEDIA || forceDespiteNoChanges);
        MpfBackupRun saved = backupRunRepository.save(run);
        Long runId = saved.getId();
        cancelSignals.put(runId, new AtomicBoolean(false));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(() -> executeBackupAsync(runId), mpfBackupExecutor);
            }
        });
        return saved;
    }

    public Optional<LocalDateTime> lastSuccessfulExcelBackupCompletedAt() {
        return lastSuccessfulBackupCompletedAt(MpfBackupKind.EXCEL);
    }

    public Optional<LocalDateTime> lastSuccessfulBackupCompletedAt(MpfBackupKind kind) {
        return backupRunRepository
                .findFirstByStatusAndBackupKindOrderByCompletedAtDesc(MpfBackupRunStatus.READY, kind)
                .map(MpfBackupRun::getCompletedAt);
    }

    public Optional<MpfBackupRun> findRun(Long id) {
        return backupRunRepository.findById(id);
    }

    public Optional<Path> resolveBackupFile(MpfBackupRun run) {
        if (run.getFilePath() == null || run.getFilePath().isBlank()) {
            return Optional.empty();
        }
        Path path = Paths.get(run.getFilePath()).normalize().toAbsolutePath();
        Path storage = MpfBackupConstants.STORAGE_DIR;
        if (!path.startsWith(storage)) {
            return Optional.empty();
        }
        return Files.exists(path) ? Optional.of(path) : Optional.empty();
    }

    private void executeBackupAsync(Long runId) {
        MpfBackupRun run = backupRunRepository.findById(runId).orElse(null);
        if (run == null || shouldAbort(runId)) {
            cancelSignals.remove(runId);
            return;
        }
        Path workDir = null;
        Path zipPath = null;
        MpfBackupKind kind = run.getBackupKind() != null ? run.getBackupKind() : MpfBackupKind.EXCEL;
        try {
            Files.createDirectories(MpfBackupConstants.STORAGE_DIR);
            workDir = Files.createTempDirectory("mpf-backup-work-");
            if (shouldAbort(runId)) {
                return;
            }
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("backupKind", kind.name());
            manifest.put("backupRunId", run.getId());

            if (kind == MpfBackupKind.EXCEL) {
                Path excelDir = workDir.resolve("excel");
                Files.createDirectories(excelDir);
                List<String> excelFiles = excelExportService.exportAllExcelFiles(excelDir);
                if (shouldAbort(runId)) {
                    return;
                }
                manifest.put("excelFiles", excelFiles);
                manifest.put("readme", "Admin text/data — open .xlsx files in Excel.");
            } else {
                Path mediaDir = workDir.resolve("media");
                Files.createDirectories(mediaDir);
                copyMediaRoots(mediaDir);
                if (shouldAbort(runId)) {
                    return;
                }
                manifest.put("readme", "Image and file uploads from MPF admin.");
            }

            Path manifestFile = workDir.resolve("manifest.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);

            if (shouldAbort(runId)) {
                return;
            }

            String stamp = LocalDateTime.now().format(FILE_STAMP);
            String prefix = kind == MpfBackupKind.EXCEL ? "mpf-data-excel-" : "mpf-media-";
            zipPath = MpfBackupConstants.STORAGE_DIR.resolve(prefix + stamp + ".zip");
            zipDirectory(workDir, zipPath);
            if (shouldAbort(runId)) {
                Files.deleteIfExists(zipPath);
                return;
            }
            String sha256 = sha256Hex(zipPath);

            run = backupRunRepository.findById(runId).orElse(run);
            if (shouldAbort(runId)) {
                Files.deleteIfExists(zipPath);
                return;
            }
            run.setFilePath(zipPath.toString());
            run.setFileSizeBytes(Files.size(zipPath));
            run.setSha256(sha256);
            run.setStatus(MpfBackupRunStatus.READY);
            run.setCompletedAt(LocalDateTime.now());
            backupRunRepository.save(run);
            purgeOldBackups();
            log.info("MPF backup run {} completed: {}", runId, zipPath);
        } catch (Exception e) {
            if (!shouldAbort(runId)) {
                log.error("MPF backup run {} failed", runId, e);
                failRun(runId, truncate(e.getMessage(), 1900));
            }
        } finally {
            cancelSignals.remove(runId);
            if (workDir != null) {
                deleteRecursive(workDir);
            }
            if (shouldAbort(runId) && zipPath != null) {
                try {
                    Files.deleteIfExists(zipPath);
                } catch (IOException ignored) {
                    // best effort
                }
            }
        }
    }

    private void markCancelled(MpfBackupRun run, String reason) {
        cancelSignals.computeIfAbsent(run.getId(), id -> new AtomicBoolean()).set(true);
        run.setStatus(MpfBackupRunStatus.CANCELLED);
        run.setErrorMessage(reason);
        run.setCompletedAt(LocalDateTime.now());
        backupRunRepository.save(run);
    }

    private void failRun(Long runId, String message) {
        backupRunRepository.findById(runId).ifPresent(run -> {
            run.setStatus(MpfBackupRunStatus.FAILED);
            run.setErrorMessage(message);
            run.setCompletedAt(LocalDateTime.now());
            backupRunRepository.save(run);
        });
    }

    private boolean shouldAbort(Long runId) {
        AtomicBoolean signal = cancelSignals.get(runId);
        if (signal != null && signal.get()) {
            return true;
        }
        return backupRunRepository
                .findById(runId)
                .map(r -> r.getStatus() == MpfBackupRunStatus.CANCELLED)
                .orElse(true);
    }

    private boolean tryMysqldump(Path sqlFile) {
        try {
            runMysqldump(sqlFile);
            return Files.exists(sqlFile) && Files.size(sqlFile) > 0;
        } catch (Exception e) {
            log.warn("mysqldump skipped (Excel + media backup will still run): {}", e.getMessage());
            try {
                Files.writeString(
                        sqlFile.getParent().resolve("README-database.txt"),
                        "SQL dump was not created. Install mysqldump on the server or use Excel exports in /excel.\n"
                                + "Error: "
                                + e.getMessage());
            } catch (IOException ignored) {
                // best effort
            }
            return false;
        }
    }

    private void runMysqldump(Path sqlFile) throws IOException, InterruptedException {
        DbCredentials creds = resolveDbCredentials();
        List<String> command = new ArrayList<>();
        command.add(MpfBackupConstants.MYSQLDUMP_COMMAND);
        command.add("--single-transaction");
        command.add("--routines");
        command.add("--triggers");
        command.add("-h");
        command.add(creds.host());
        command.add("-P");
        command.add(String.valueOf(creds.port()));
        command.add("-u");
        command.add(creds.username());
        if (creds.password() != null && !creds.password().isEmpty()) {
            command.add("-p" + creds.password());
        }
        command.add(creds.database());
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(sqlFile.toFile());
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        Process process = pb.start();
        String err = new String(process.getErrorStream().readAllBytes());
        int code = process.waitFor();
        if (code != 0) {
            throw new IOException("mysqldump failed (exit " + code + "): " + err);
        }
    }

    private DbCredentials resolveDbCredentials() {
        if (dataSource instanceof HikariDataSource hikari) {
            String jdbc = hikari.getJdbcUrl();
            return parseJdbc(jdbc, hikari.getUsername(), hikari.getPassword());
        }
        throw new IllegalStateException("Unsupported DataSource for backup");
    }

    private static DbCredentials parseJdbc(String jdbcUrl, String user, String pass) {
        String noPrefix = jdbcUrl.replace("jdbc:mysql://", "");
        String hostPortDb = noPrefix.split("\\?")[0];
        String[] hostAndRest = hostPortDb.split("/", 2);
        String hostPort = hostAndRest[0];
        String database = hostAndRest.length > 1 ? hostAndRest[1] : "mypropertyfact";
        String host = hostPort;
        int port = 3306;
        if (hostPort.contains(":")) {
            String[] hp = hostPort.split(":", 2);
            host = hp[0];
            port = Integer.parseInt(hp[1]);
        }
        return new DbCredentials(host, port, database, user, pass);
    }

    private void copyMediaRoots(Path mediaDir) throws IOException {
        Map<String, String> roots = linkedMediaRoots();
        for (Map.Entry<String, String> e : roots.entrySet()) {
            Path src = Paths.get(e.getValue()).normalize();
            if (!Files.exists(src)) {
                continue;
            }
            Path dest = mediaDir.resolve(e.getKey());
            copyTree(src, dest);
        }
    }

    private Map<String, String> linkedMediaRoots() {
        Map<String, String> roots = new LinkedHashMap<>();
        putIfPresent(roots, "upload_dir", uploadDir);
        putIfPresent(roots, "uploads_path", uploadsPath);
        putIfPresent(roots, "upload_amenity_path", uploadAmenityPath);
        putIfPresent(roots, "upload_icon_path", uploadIconPath);
        putIfPresent(roots, "upload.dir", uploadDotDir);
        return roots;
    }

    private static void putIfPresent(Map<String, String> map, String key, String path) {
        if (path != null && !path.isBlank()) {
            map.put(key, path.trim());
        }
    }

    private Map<String, Object> buildManifest(
            MpfBackupRun run,
            Path sqlFile,
            boolean sqlOk,
            Path excelDir,
            List<String> excelFiles,
            Path mediaDir)
            throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("backupRunId", run.getId());
        manifest.put("createdAt", run.getCreatedAt() != null ? run.getCreatedAt().toString() : null);
        manifest.put("trigger", run.getTriggerType() != null ? run.getTriggerType().name() : null);
        manifest.put("databaseFile", sqlOk ? "database/mypropertyfact.sql" : null);
        manifest.put("databaseBytes", sqlOk && Files.exists(sqlFile) ? Files.size(sqlFile) : 0);
        manifest.put("excelFiles", excelFiles);
        manifest.put("excelFolder", "excel/");
        if (Files.exists(excelDir)) {
            try (Stream<Path> stream = Files.list(excelDir)) {
                manifest.put(
                        "excelBytes",
                        stream.filter(Files::isRegularFile).mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return 0L;
                            }
                        }).sum());
            }
        }
        List<Map<String, Object>> mediaEntries = new ArrayList<>();
        if (Files.exists(mediaDir)) {
            try (Stream<Path> stream = Files.list(mediaDir)) {
                stream.forEach(p -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("label", p.getFileName().toString());
                    try {
                        entry.put("bytes", directorySize(p));
                    } catch (IOException ex) {
                        entry.put("bytes", 0);
                    }
                    mediaEntries.add(entry);
                });
            }
        }
        manifest.put("mediaRoots", mediaEntries);
        return manifest;
    }

    private static long directorySize(Path root) throws IOException {
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    return 0L;
                }
            }).sum();
        }
    }

    private void zipDirectory(Path sourceDir, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath));
                Stream<Path> walk = Files.walk(sourceDir)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                String entryName =
                        sourceDir.relativize(file).toString().replace('\\', '/');
                try {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static String sha256Hex(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file));
                DigestInputStream din = new DigestInputStream(in, digest)) {
            byte[] buf = new byte[8192];
            while (din.read(buf) != -1) {
                // drain
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void purgeOldBackups() throws IOException {
        LocalDateTime cutoff =
                LocalDateTime.now().minusWeeks(MpfBackupConstants.RETENTION_WEEKS);
        try (Stream<Path> files = Files.list(MpfBackupConstants.STORAGE_DIR)) {
            files.filter(p -> {
                        String n = p.getFileName().toString();
                        return (n.startsWith("mpf-data-excel-") || n.startsWith("mpf-media-")
                                        || n.startsWith("mpf-backup-"))
                                && n.endsWith(".zip");
                    })
                    .forEach(p -> {
                        try {
                            LocalDateTime mtime =
                                    LocalDateTime.ofInstant(
                                            Files.getLastModifiedTime(p).toInstant(),
                                            java.time.ZoneId.systemDefault());
                            if (mtime.isBefore(cutoff)) {
                                Files.deleteIfExists(p);
                            }
                        } catch (IOException e) {
                            log.warn("Could not purge old backup {}: {}", p, e.getMessage());
                        }
                    });
        }
        List<MpfBackupRun> oldRuns =
                backupRunRepository.findByStatusAndCreatedAtBefore(
                        MpfBackupRunStatus.READY, cutoff);
        for (MpfBackupRun run : oldRuns) {
            if (run.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(run.getFilePath()));
                } catch (IOException ignored) {
                    // best effort
                }
            }
        }
    }

    private static void copyTree(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void deleteRecursive(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException ignored) {
            // best effort
        }
    }

    private static String truncate(String msg, int max) {
        if (msg == null) {
            return null;
        }
        return msg.length() <= max ? msg : msg.substring(0, max);
    }

    private record DbCredentials(String host, int port, String database, String username, String password) {}
}
