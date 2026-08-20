package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.AdminDashboardActivityItemDto;
import com.mypropertyfact.estate.entities.AdminDashboardActivity;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.AdminDashboardActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardActivityService {

    private static final int TITLE_MAX = 480;

    private final AdminDashboardActivityRepository repository;

    /**
     * Task type codes consumed by the admin dashboard UI.
     */
    public static final String TASK_BLOG = "BLOG";
    public static final String TASK_BLOG_CATEGORY = "BLOG_CATEGORY";
    public static final String TASK_PROPERTY_APPROVED = "PROPERTY_APPROVED";
    public static final String TASK_PROPERTY_REJECTED = "PROPERTY_REJECTED";

    @Transactional
    public void recordForCurrentUser(String taskType, String title, String href) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user) || user.getId() == null) {
            return;
        }
        if (taskType == null || taskType.isBlank()) {
            return;
        }
        String safeTitle = truncate(title != null ? title : "", TITLE_MAX);
        if (safeTitle.isEmpty()) {
            safeTitle = "(untitled)";
        }
        String safeHref = href != null && href.length() > 600 ? href.substring(0, 600) : href;

        try {
            AdminDashboardActivity row = new AdminDashboardActivity();
            row.setActorUserId(user.getId());
            row.setTaskType(taskType.trim().toUpperCase());
            row.setTitle(safeTitle);
            row.setHref(safeHref);
            repository.save(row);
        } catch (Exception e) {
            log.warn("Could not record admin dashboard activity: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AdminDashboardActivityItemDto> getRecentForUser(Integer userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        int cap = Math.min(Math.max(limit, 1), 50);
        return repository
                .findByActorUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, cap))
                .stream()
                .map(
                        a ->
                                new AdminDashboardActivityItemDto(
                                        a.getTaskType(),
                                        a.getTitle(),
                                        a.getHref(),
                                        a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
