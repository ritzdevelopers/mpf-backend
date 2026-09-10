package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.UserActivityItemDto;
import com.mypropertyfact.estate.dtos.UserActivitySummaryResponse;
import com.mypropertyfact.estate.dtos.UserActivityUpsertRequest;
import com.mypropertyfact.estate.entities.UserSiteActivity;
import com.mypropertyfact.estate.repositories.UserSiteActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserSiteActivityService {

    private final UserSiteActivityRepository repository;

    @Transactional(readOnly = true)
    public UserActivitySummaryResponse summary(Integer userId) {
        List<UserSiteActivity> recent = repository.findTop40ByUserIdOrderByUpdatedAtDesc(userId);
        return UserActivitySummaryResponse.builder()
                .viewedCount(repository.countByUserIdAndActivityType(userId, "VIEW"))
                .shortlistedCount(repository.countByUserIdAndActivityType(userId, "SHORTLIST"))
                .searchCount(repository.countByUserIdAndActivityType(userId, "SEARCH"))
                .recent(recent.stream().map(this::toDto).toList())
                .build();
    }

    @Transactional
    public UserActivitySummaryResponse upsert(Integer userId, UserActivityUpsertRequest request) {
        String type = normalize(request.getType(), "VIEW");
        String entityType = normalize(request.getEntityType(), "PROJECT");
        String slug = trimToEmpty(request.getEntitySlug());
        if (slug.isBlank()) {
            slug = trimToEmpty(request.getEntityId());
        }
        String action = trimToEmpty(request.getAction()).toLowerCase(Locale.ROOT);

        if ("SHORTLIST".equals(type) && "remove".equals(action)) {
            repository.deleteByUserIdAndActivityTypeAndEntityTypeAndEntitySlug(
                    userId, type, entityType, slug);
            return summary(userId);
        }

        UserSiteActivity row = repository
                .findFirstByUserIdAndActivityTypeAndEntityTypeAndEntitySlug(
                        userId, type, entityType, slug)
                .orElseGet(UserSiteActivity::new);
        row.setUserId(userId);
        row.setActivityType(type);
        row.setEntityType(entityType);
        row.setEntityId(trimToEmpty(request.getEntityId()));
        row.setEntitySlug(slug);
        row.setEntityLabel(trimToEmpty(request.getEntityLabel()));
        row.setHref(trimToEmpty(request.getHref()));
        repository.save(row);
        return summary(userId);
    }

    private UserActivityItemDto toDto(UserSiteActivity row) {
        return UserActivityItemDto.builder()
                .id(row.getId())
                .type(row.getActivityType())
                .entityType(row.getEntityType())
                .entityId(row.getEntityId())
                .entitySlug(row.getEntitySlug())
                .entityLabel(row.getEntityLabel())
                .href(row.getHref())
                .at(row.getUpdatedAt() != null ? row.getUpdatedAt() : row.getCreatedAt())
                .build();
    }

    private String normalize(String value, String fallback) {
        String trimmed = trimToEmpty(value).toUpperCase(Locale.ROOT);
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
