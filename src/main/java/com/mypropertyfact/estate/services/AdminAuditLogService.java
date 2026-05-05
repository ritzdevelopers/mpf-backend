package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.AdminAuditLogEntryDto;
import com.mypropertyfact.estate.entities.AdminAuditLog;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.AdminAuditLogRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import com.mypropertyfact.estate.specs.AdminAuditLogSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    public static final int REQUEST_PATH_MAX = 1000;
    public static final int QUERY_MAX = 256;
    public static final int METHOD_MAX = 16;

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final UserRepository userRepository;

    public static final int TASK_LABEL_MAX = 512;
    public static final int CLIENT_PAGE_MAX = 512;

    @Transactional
    public void record(
            String actorEmail,
            Integer actorUserId,
            String httpMethod,
            String requestPath,
            String queryString,
            int httpStatus,
            int durationMs,
            String taskLabel,
            String clientAdminPage,
            Integer clientDwellMs) {
        AdminAuditLog row = new AdminAuditLog();
        row.setOccurredAt(LocalDateTime.now());
        row.setActorEmail(truncate(actorEmail != null ? actorEmail : "", 255));
        row.setActorUserId(actorUserId);
        row.setHttpMethod(truncate(httpMethod != null ? httpMethod : "", METHOD_MAX));
        row.setRequestPath(truncate(requestPath != null ? requestPath : "", REQUEST_PATH_MAX));
        String q = queryString == null || queryString.isBlank() ? null : truncate(queryString, QUERY_MAX);
        row.setQueryString(q);
        row.setHttpStatus(httpStatus);
        row.setSuccess(httpStatus >= 200 && httpStatus < 400);
        row.setDurationMs(Math.max(0, durationMs));
        row.setTaskLabel(truncate(taskLabel != null ? taskLabel : "", TASK_LABEL_MAX));
        row.setClientAdminPage(
                clientAdminPage == null || clientAdminPage.isBlank()
                        ? null
                        : truncate(clientAdminPage, CLIENT_PAGE_MAX));
        row.setClientDwellMs(clientDwellMs);
        adminAuditLogRepository.save(row);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogEntryDto> search(
            String fromIso,
            String toIso,
            String email,
            Boolean success,
            String pathContains,
            Pageable pageable) {
        LocalDateTime from = parseIsoLocal(fromIso);
        LocalDateTime to = parseIsoLocal(toIso);
        Specification<AdminAuditLog> spec = Specification.allOf(
                AdminAuditLogSpecs.occurredAtFrom(from),
                AdminAuditLogSpecs.occurredAtTo(to),
                AdminAuditLogSpecs.actorEmailContains(email),
                AdminAuditLogSpecs.successEqual(success),
                AdminAuditLogSpecs.pathTaskOrClientContains(pathContains)
        );
        Page<AdminAuditLog> page = adminAuditLogRepository.findAll(spec, pageable);
        Set<Integer> userIds = new HashSet<>();
        for (AdminAuditLog row : page.getContent()) {
            if (row.getActorUserId() != null) {
                userIds.add(row.getActorUserId());
            }
        }
        Map<Integer, String> idToFullName = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userRepository.findAllById(userIds)) {
                String fn = u.getFullName();
                idToFullName.put(
                        u.getId(),
                        fn != null && !fn.isBlank() ? fn.trim() : null);
            }
        }
        return page.map(e -> toDto(e, idToFullName));
    }

    private AdminAuditLogEntryDto toDto(AdminAuditLog e, Map<Integer, String> idToFullName) {
        String label = e.getTaskLabel();
        if (label == null || label.isBlank()) {
            label = AdminApiTaskDescriber.describe(e.getHttpMethod(), e.getRequestPath());
        }
        String actorFullName = null;
        if (e.getActorUserId() != null) {
            actorFullName = idToFullName.get(e.getActorUserId());
        }
        return AdminAuditLogEntryDto.builder()
                .id(e.getId())
                .occurredAt(e.getOccurredAt())
                .actorEmail(e.getActorEmail())
                .actorFullName(actorFullName)
                .actorUserId(e.getActorUserId())
                .httpMethod(e.getHttpMethod())
                .requestPath(e.getRequestPath())
                .queryString(e.getQueryString())
                .httpStatus(e.getHttpStatus())
                .success(e.isSuccess())
                .durationMs(e.getDurationMs())
                .taskLabel(label)
                .clientAdminPage(e.getClientAdminPage())
                .clientDwellMs(e.getClientDwellMs())
                .build();
    }

    private static LocalDateTime parseIsoLocal(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(iso);
            } catch (DateTimeParseException ignored2) {
                return null;
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
