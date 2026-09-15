package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.ListingActivityEvent;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.ListingActivityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingActivityService {

    public static final String SOURCE_PROJECT = "PROJECT";
    public static final String SOURCE_PORTAL = "PORTAL";

    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_UPDATED = "UPDATED";
    public static final String ACTION_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String ACTION_APPROVED = "APPROVED";
    public static final String ACTION_REJECTED = "REJECTED";
    public static final String ACTION_PUBLISHED = "PUBLISHED";
    public static final String ACTION_UNPUBLISHED = "UNPUBLISHED";

    private final ListingActivityEventRepository repository;

    public static User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    @Transactional
    public void record(String source, Long entityId, String action, String detail) {
        User actor = currentUserOrNull();
        record(source, entityId, action, actor, detail);
    }

    @Transactional
    public void record(String source, Long entityId, String action, User actor, String detail) {
        if (source == null || entityId == null || action == null || action.isBlank()) {
            return;
        }
        try {
            ListingActivityEvent row = new ListingActivityEvent();
            row.setSource(source.trim().toUpperCase());
            row.setEntityId(entityId);
            row.setAction(action.trim().toUpperCase());
            row.setDetail(truncate(detail, 500));
            if (actor != null) {
                row.setActorUserId(actor.getId());
                row.setActorName(firstNonBlank(actor.getFullName(), actor.getEmail(), "Unknown"));
            } else {
                row.setActorName("System");
            }
            repository.save(row);
        } catch (Exception e) {
            log.warn("Could not record listing activity {} {} {}: {}", source, entityId, action, e.getMessage());
        }
    }

    public static void addIfChanged(List<String> changes, String label, Object before, Object after) {
        if (changes == null || label == null || label.isBlank()) {
            return;
        }
        if (changed(before, after)) {
            changes.add(label);
        }
    }

    public static void addIfProvidedAndChanged(List<String> changes, String label, Object before, Object after) {
        if (after == null) {
            return;
        }
        addIfChanged(changes, label, before, after);
    }

    public static String describeChanges(List<String> changes, String fallback) {
        if (changes == null || changes.isEmpty()) {
            return firstNonBlank(fallback, "Updated listing");
        }
        if (changes.size() == 1) {
            return "Changed " + changes.get(0);
        }
        if (changes.size() == 2) {
            return "Changed " + changes.get(0) + " and " + changes.get(1);
        }
        int keep = Math.min(changes.size(), 5);
        String joined = String.join(", ", changes.subList(0, keep - 1));
        String last = changes.get(keep - 1);
        String extra = changes.size() > keep ? " and more" : "";
        return "Changed " + joined + ", and " + last + extra;
    }

    public static boolean changed(Object before, Object after) {
        return !normalize(before).equalsIgnoreCase(normalize(after));
    }

    private static String normalize(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed.isEmpty() ? null : trimmed;
        }
        return trimmed.substring(0, max);
    }
}
