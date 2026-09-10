package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.PortalListingStatsResponse;
import com.mypropertyfact.estate.dtos.PortalListingStatsRowDto;
import com.mypropertyfact.estate.dtos.PortalListingStatsSummaryDto;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.enums.ProjectApprovalStatus;
import com.mypropertyfact.estate.repositories.PropertyListingRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortalListingStatsService {

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PropertyListingRepository propertyListingRepository;

    public PortalListingStatsResponse build() {
        List<User> users = userRepository.findPortalBrokerOwnerUsers();
        List<Integer> userIds = users.stream().map(User::getId).toList();

        Map<Integer, Map<ProjectApprovalStatus, Long>> countsByUser = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Object[] row : propertyListingRepository.countGroupedByUserIdAndStatus(userIds)) {
                Integer userId = (Integer) row[0];
                ProjectApprovalStatus status = (ProjectApprovalStatus) row[1];
                Long count = (Long) row[2];
                countsByUser
                        .computeIfAbsent(userId, ignored -> new HashMap<>())
                        .put(status, count == null ? 0L : count);
            }
        }

        List<PortalListingStatsRowDto> rows = new ArrayList<>();
        long brokers = 0;
        long owners = 0;
        long totalListings = 0;
        long liveListings = 0;
        long pendingListings = 0;
        long draftListings = 0;
        long rejectedListings = 0;

        for (User user : users) {
            String userType = userRoleService.resolvePortalPersonaLabel(user);
            if ("OWNER".equals(userType)) {
                owners += 1;
            } else {
                brokers += 1;
            }

            Map<ProjectApprovalStatus, Long> byStatus = countsByUser.getOrDefault(user.getId(), Map.of());
            long live = byStatus.getOrDefault(ProjectApprovalStatus.APPROVED, 0L);
            long pending = byStatus.getOrDefault(ProjectApprovalStatus.PENDING, 0L);
            long draft = byStatus.getOrDefault(ProjectApprovalStatus.DRAFT, 0L);
            long rejected = byStatus.getOrDefault(ProjectApprovalStatus.REJECTED, 0L);
            long total = live + pending + draft + rejected;

            totalListings += total;
            liveListings += live;
            pendingListings += pending;
            draftListings += draft;
            rejectedListings += rejected;

            rows.add(new PortalListingStatsRowDto(
                    user.getId(),
                    blankToDash(user.getFullName()),
                    blankToDash(user.getEmail()),
                    blankToDash(user.getPhone()),
                    userType,
                    blankToDash(user.getUserCategory()),
                    Boolean.TRUE.equals(user.getEnabled()),
                    Boolean.TRUE.equals(user.getVerified()),
                    total,
                    live,
                    pending,
                    draft,
                    rejected
            ));
        }

        rows.sort(Comparator
                .comparingLong(PortalListingStatsRowDto::totalListings)
                .reversed()
                .thenComparing(row -> row.fullName() == null ? "" : row.fullName(), String.CASE_INSENSITIVE_ORDER));

        return new PortalListingStatsResponse(
                new PortalListingStatsSummaryDto(
                        users.size(),
                        brokers,
                        owners,
                        totalListings,
                        liveListings,
                        pendingListings,
                        draftListings,
                        rejectedListings
                ),
                rows
        );
    }

    private static String blankToDash(String value) {
        if (value == null || value.isBlank()) return "—";
        return value.trim();
    }
}
