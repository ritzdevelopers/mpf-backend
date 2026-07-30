package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.AdminAuditLogEntryDto;
import com.mypropertyfact.estate.dtos.AdminManagementActivityItemDto;
import com.mypropertyfact.estate.dtos.AdminManagementActivityPageResponse;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminManagementActivityService {

    private final AdminAuditLogService adminAuditLogService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminManagementActivityPageResponse list(Pageable pageable) {
        Page<AdminAuditLogEntryDto> page = adminAuditLogService.search(
                null,
                null,
                null,
                null,
                null,
                pageable);

        Set<Integer> userIds = new HashSet<>();
        for (AdminAuditLogEntryDto row : page.getContent()) {
            if (row.getActorUserId() != null) {
                userIds.add(row.getActorUserId());
            }
        }
        Map<Integer, String> idToName = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userRepository.findAllById(userIds)) {
                String name = u.getFullName();
                idToName.put(u.getId(), name != null && !name.isBlank() ? name.trim() : "");
            }
        }

        var items = page.getContent().stream()
                .map(row -> toItem(row, idToName))
                .toList();

        return AdminManagementActivityPageResponse.builder()
                .content(items)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .number(page.getNumber())
                .size(page.getSize())
                .build();
    }

    private static AdminManagementActivityItemDto toItem(
            AdminAuditLogEntryDto row,
            Map<Integer, String> idToName) {
        String email = row.getActorEmail() != null ? row.getActorEmail().trim() : "";
        String displayName = resolveDisplayName(row.getActorUserId(), email, idToName);
        String task = row.getTaskLabel() != null && !row.getTaskLabel().isBlank()
                ? row.getTaskLabel().trim()
                : "Admin action";
        String event;
        if (!email.isEmpty()) {
            event = "User " + displayName + " (" + email + ") — " + task;
        } else {
            event = "User " + displayName + " — " + task;
        }
        return AdminManagementActivityItemDto.builder()
                .occurredAt(row.getOccurredAt())
                .event(event)
                .actorName(displayName)
                .actorEmail(email.isEmpty() ? null : email)
                .actorUserId(row.getActorUserId())
                .action(task)
                .httpMethod(row.getHttpMethod())
                .success(row.isSuccess())
                .requestPath(row.getRequestPath())
                .build();
    }

    private static String resolveDisplayName(
            Integer actorUserId,
            String email,
            Map<Integer, String> idToName) {
        if (actorUserId != null) {
            String n = idToName.get(actorUserId);
            if (n != null && !n.isBlank()) {
                return n;
            }
        }
        if (!email.isEmpty()) {
            int at = email.indexOf('@');
            return at > 0 ? email.substring(0, at) : email;
        }
        return "Unknown";
    }
}
