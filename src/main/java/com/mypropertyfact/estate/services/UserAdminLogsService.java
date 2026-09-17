package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.UserActivitySummaryResponse;
import com.mypropertyfact.estate.dtos.UserAdminLoginLogDto;
import com.mypropertyfact.estate.dtos.UserAdminLogsResponse;
import com.mypropertyfact.estate.entities.MasterRole;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.entities.WebsiteLoginEvent;
import com.mypropertyfact.estate.repositories.UserRepository;
import com.mypropertyfact.estate.repositories.WebsiteLoginEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminLogsService {

    private static final int LOGIN_LIMIT = 50;

    private final UserRepository userRepository;
    private final UserSiteActivityService userSiteActivityService;
    private final WebsiteLoginEventRepository websiteLoginEventRepository;

    @Transactional(readOnly = true)
    public Optional<UserAdminLogsResponse> build(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId).map(this::toResponse);
    }

    private UserAdminLogsResponse toResponse(User user) {
        UserActivitySummaryResponse activity = userSiteActivityService.summary(user.getId());
        List<WebsiteLoginEvent> logins = loadLogins(user);
        long loginCount = logins.size();
        try {
            loginCount = Math.max(loginCount, websiteLoginEventRepository.countByUserId(user.getId()));
        } catch (DataAccessException ex) {
            log.warn("Could not count website logins for user {}: {}", user.getId(), ex.getMessage());
        }
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .filter(Objects::nonNull)
                        .map(MasterRole::getRoleName)
                        .filter(name -> name != null && !name.isBlank())
                        .sorted()
                        .toList();

        return UserAdminLogsResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userCategory(user.getUserCategory())
                .enabled(user.getEnabled())
                .verified(user.getVerified())
                .createdAt(toLocalDateTime(user.getCreatedAt()))
                .updatedAt(toLocalDateTime(user.getUpdatedAt()))
                .roles(roles)
                .viewedCount(activity.getViewedCount())
                .shortlistedCount(activity.getShortlistedCount())
                .searchCount(activity.getSearchCount())
                .loginCount(loginCount)
                .recentActivity(activity.getRecent() == null ? List.of() : activity.getRecent())
                .recentLogins(logins.stream().limit(LOGIN_LIMIT).map(this::toLoginDto).toList())
                .build();
    }

    private List<WebsiteLoginEvent> loadLogins(User user) {
        try {
            List<WebsiteLoginEvent> byId = websiteLoginEventRepository
                    .findTop50ByUserIdOrderByLoggedInAtDesc(user.getId());
            List<WebsiteLoginEvent> byEmail = user.getEmail() == null || user.getEmail().isBlank()
                    ? List.of()
                    : websiteLoginEventRepository
                            .findTop50ByEmailIgnoreCaseOrderByLoggedInAtDesc(user.getEmail().trim());
            Map<Long, WebsiteLoginEvent> merged = new LinkedHashMap<>();
            Stream.concat(byId.stream(), byEmail.stream())
                    .filter(event -> event != null && event.getId() != null)
                    .sorted(Comparator.comparing(
                            WebsiteLoginEvent::getLoggedInAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .forEach(event -> merged.putIfAbsent(event.getId(), event));
            return new ArrayList<>(merged.values());
        } catch (DataAccessException ex) {
            log.warn("Could not load website logins for user {}: {}", user.getId(), ex.getMessage());
            return List.of();
        }
    }

    private UserAdminLoginLogDto toLoginDto(WebsiteLoginEvent event) {
        return UserAdminLoginLogDto.builder()
                .id(event.getId())
                .loggedInAt(event.getLoggedInAt())
                .source(event.getSource())
                .locationLabel(event.getLocationLabel())
                .accountStatus(event.getAccountStatus())
                .userAgent(event.getUserAgent())
                .build();
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
