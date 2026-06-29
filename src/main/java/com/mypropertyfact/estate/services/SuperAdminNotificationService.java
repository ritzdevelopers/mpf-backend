package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.SuperAdminNotificationItemDto;
import com.mypropertyfact.estate.dtos.SuperAdminNotificationsResponse;
import com.mypropertyfact.estate.entities.AdminAuditLog;
import com.mypropertyfact.estate.entities.Enquery;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.repositories.AdminAuditLogRepository;
import com.mypropertyfact.estate.repositories.EnqueryRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SuperAdminNotificationService {

    private static final int AUDIT_SCAN = 120;
    private static final int ENQUIRY_SCAN = 30;
    private static final int MAX_RETURN = 25;
    private static final int LOOKBACK_DAYS = 14;

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final EnqueryRepository enqueryRepository;
    private final UserRepository userRepository;
    private final AdminPasswordResetRequestService adminPasswordResetRequestService;

    @Transactional(readOnly = true)
    public SuperAdminNotificationsResponse buildFeed(LocalDateTime since) {
        LocalDateTime cutoff = since != null ? since : LocalDateTime.now().minusDays(LOOKBACK_DAYS);
        List<SuperAdminNotificationItemDto> items = new ArrayList<>();

        items.addAll(fromRecentEnquiries(cutoff));
        items.addAll(fromAuditLogs(cutoff));
        items.addAll(fromPendingPermissions());

        items.sort(Comparator.comparing(
                SuperAdminNotificationItemDto::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        if (items.size() > MAX_RETURN) {
            items = new ArrayList<>(items.subList(0, MAX_RETURN));
        }

        int unread = (int) items.stream().filter(SuperAdminNotificationItemDto::isUnread).count();
        int pendingCount = adminPasswordResetRequestService.countPending().getTotalPending();

        return SuperAdminNotificationsResponse.builder()
                .notifications(items)
                .unreadCount(unread)
                .pendingPermissionsCount(pendingCount)
                .build();
    }

    private List<SuperAdminNotificationItemDto> fromPendingPermissions() {
        var counts = adminPasswordResetRequestService.countPending();
        List<SuperAdminNotificationItemDto> out = new ArrayList<>();
        if (counts.getTotalPending() <= 0) {
            return out;
        }
        StringBuilder msg = new StringBuilder("Items waiting for your review");
        if (counts.getAdminAccessPending() > 0) {
            msg.append(": ")
                    .append(counts.getAdminAccessPending())
                    .append(" portal registration")
                    .append(counts.getAdminAccessPending() == 1 ? "" : "s");
        }
        if (counts.getPasswordChangePending() > 0) {
            if (counts.getAdminAccessPending() > 0) {
                msg.append(" and ");
            } else {
                msg.append(": ");
            }
            msg.append(counts.getPasswordChangePending())
                    .append(" password change request")
                    .append(counts.getPasswordChangePending() == 1 ? "" : "s");
        }
        out.add(SuperAdminNotificationItemDto.builder()
                .id("permission:pending")
                .type("PERMISSION")
                .title("Pending permissions")
                .message(msg.toString())
                .actorName("System")
                .taskLabel("Review portal registrations and password requests")
                .occurredAt(LocalDateTime.now())
                .href("/admin/dashboard/pending-permissions")
                .unread(true)
                .build());
        return out;
    }

    private List<SuperAdminNotificationItemDto> fromRecentEnquiries(LocalDateTime cutoff) {
        List<Enquery> rows = enqueryRepository.findByCreatedAtAfterOrderByCreatedAtDesc(cutoff);
        List<SuperAdminNotificationItemDto> out = new ArrayList<>();
        int limit = Math.min(rows.size(), ENQUIRY_SCAN);
        for (int i = 0; i < limit; i++) {
            Enquery e = rows.get(i);
            String name = blankTo(e.getName(), "Someone");
            String source = blankTo(e.getEnquiryFrom(), "website");
            String page = blankTo(e.getPageName(), blankTo(e.getProjectLink(), "a property page"));
            out.add(SuperAdminNotificationItemDto.builder()
                    .id("enquiry:" + e.getId())
                    .type("ENQUIRY")
                    .title("New enquiry received")
                    .message(name + " submitted an enquiry via " + source + " for " + page)
                    .actorName(name)
                    .taskLabel("New lead / enquiry")
                    .occurredAt(e.getCreatedAt())
                    .href("/admin/dashboard/enquiries")
                    .unread(true)
                    .build());
        }
        return out;
    }

    private List<SuperAdminNotificationItemDto> fromAuditLogs(LocalDateTime cutoff) {
        List<AdminAuditLog> rows = adminAuditLogRepository.findBySuccessTrueAndOccurredAtAfterOrderByOccurredAtDesc(
                cutoff,
                PageRequest.of(0, AUDIT_SCAN, Sort.by(Sort.Direction.DESC, "occurredAt")));

        Set<Integer> userIds = new HashSet<>();
        for (AdminAuditLog row : rows) {
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

        List<SuperAdminNotificationItemDto> out = new ArrayList<>();
        for (AdminAuditLog row : rows) {
            NotificationKind kind = classifyAudit(row);
            if (kind == null) {
                continue;
            }
            String actorName = resolveActorName(row, idToName);
            String task = row.getTaskLabel() != null ? row.getTaskLabel().trim() : "Admin action";
            out.add(SuperAdminNotificationItemDto.builder()
                    .id("audit:" + row.getId())
                    .type(kind.name())
                    .title(kind.title)
                    .message(actorName + " — " + task)
                    .actorName(actorName)
                    .taskLabel(task)
                    .occurredAt(row.getOccurredAt())
                    .href(kind.href)
                    .unread(true)
                    .build());
        }
        return out;
    }

    private static NotificationKind classifyAudit(AdminAuditLog row) {
        if (!row.isSuccess()) {
            return null;
        }
        String method = row.getHttpMethod() != null ? row.getHttpMethod().toUpperCase(Locale.ROOT) : "";
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return null;
        }

        String task = row.getTaskLabel() != null ? row.getTaskLabel().toLowerCase(Locale.ROOT) : "";
        String path = row.getRequestPath() != null ? row.getRequestPath().toLowerCase(Locale.ROOT) : "";

        if (task.contains("blog") || path.contains("/blog")) {
            if (task.contains("list") || task.contains("open") || task.contains("view")) {
                return null;
            }
            return NotificationKind.BLOG;
        }
        if (task.contains("enquir") || path.contains("/enquiry")) {
            if (task.contains("list") || task.contains("view")) {
                return null;
            }
            return NotificationKind.ENQUIRY;
        }
        if (task.contains("project") || task.contains("property") || task.contains("listing")
                || path.contains("/projects") || path.contains("/property-listings")) {
            if (task.contains("list") || task.contains("browse") || task.contains("export")
                    || task.contains("search") || task.contains("open") || task.contains("view")) {
                return null;
            }
            return NotificationKind.PROPERTY;
        }
        return null;
    }

    private static String resolveActorName(AdminAuditLog row, Map<Integer, String> idToName) {
        if (row.getActorUserId() != null) {
            String n = idToName.get(row.getActorUserId());
            if (n != null && !n.isBlank()) {
                return n;
            }
        }
        String email = row.getActorEmail() != null ? row.getActorEmail().trim() : "";
        if (!email.isEmpty()) {
            int at = email.indexOf('@');
            return at > 0 ? email.substring(0, at) : email;
        }
        return "Admin user";
    }

    private static String blankTo(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private enum NotificationKind {
        BLOG("Blog activity", "/admin/dashboard/manage-blogs"),
        ENQUIRY("Enquiry activity", "/admin/dashboard/enquiries"),
        PROPERTY("Property activity", "/admin/dashboard/property-approvals");

        final String title;
        final String href;

        NotificationKind(String title, String href) {
            this.title = title;
            this.href = href;
        }
    }
}
