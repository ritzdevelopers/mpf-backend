package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.LiveListingEditDto;
import com.mypropertyfact.estate.dtos.LiveListingRowDto;
import com.mypropertyfact.estate.dtos.LiveListingSummaryDto;
import com.mypropertyfact.estate.dtos.LiveListingsResponse;
import com.mypropertyfact.estate.entities.City;
import com.mypropertyfact.estate.entities.ListingActivityEvent;
import com.mypropertyfact.estate.entities.Project;
import com.mypropertyfact.estate.entities.PropertyListing;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.enums.ProjectApprovalStatus;
import com.mypropertyfact.estate.repositories.ListingActivityEventRepository;
import com.mypropertyfact.estate.repositories.ProjectRepository;
import com.mypropertyfact.estate.repositories.PropertyListingRepository;
import com.mypropertyfact.estate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LiveListingsService {

    private static final int MAX_EDITS_PER_ROW = 12;
    private static final NumberFormat INR = NumberFormat.getInstance(new Locale("en", "IN"));

    private final ProjectRepository projectRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final ListingActivityEventRepository listingActivityEventRepository;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    @Transactional(readOnly = true)
    public LiveListingsResponse build() {
        List<Project> projects = projectRepository.findLiveWithDetails();
        List<PropertyListing> listings = propertyListingRepository
                .findByApprovalStatusWithUserAndCity(ProjectApprovalStatus.APPROVED);

        Map<String, List<ListingActivityEvent>> activity = loadActivity(
                projects.stream().map(project -> (long) project.getId()).toList(),
                listings.stream().map(PropertyListing::getId).toList());
        Map<Integer, String> actorNames = loadActorNames(activity, projects, listings);

        List<LiveListingRowDto> rows = new ArrayList<>();
        for (Project project : projects) {
            rows.add(toProjectRow(
                    project,
                    activity.getOrDefault(key("PROJECT", (long) project.getId()), List.of()),
                    actorNames));
        }

        long brokerListings = 0;
        long ownerListings = 0;
        for (PropertyListing listing : listings) {
            LiveListingRowDto row = toPortalRow(
                    listing,
                    activity.getOrDefault(key("PORTAL", listing.getId()), List.of()),
                    actorNames);
            rows.add(row);
            if ("OWNER".equals(row.listerType())) {
                ownerListings += 1;
            } else {
                brokerListings += 1;
            }
        }

        rows.sort(Comparator
                .comparing(LiveListingRowDto::wentLiveAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(row -> row.title() == null ? "" : row.title(), String.CASE_INSENSITIVE_ORDER));

        long websiteProjects = rows.stream().filter(row -> "PROJECT".equals(row.source())).count();
        long portalListings = brokerListings + ownerListings;

        return new LiveListingsResponse(
                new LiveListingSummaryDto(
                        websiteProjects + portalListings,
                        websiteProjects,
                        portalListings,
                        brokerListings,
                        ownerListings
                ),
                rows
        );
    }

    private LiveListingRowDto toProjectRow(
            Project project,
            List<ListingActivityEvent> events,
            Map<Integer, String> actorNames) {
        String city = project.getCity() != null ? blankToNull(project.getCity().getName()) : null;
        String builder = project.getBuilder() != null ? blankToNull(project.getBuilder().getBuilderName()) : null;
        String type = project.getProjectTypes() != null
                ? blankToNull(project.getProjectTypes().getProjectTypeName())
                : null;
        String statusName = project.getProjectStatus() != null
                ? blankToNull(project.getProjectStatus().getStatusName())
                : "Live";
        String slug = blankToNull(project.getSlugURL());
        LocalDateTime wentLive = firstNonNull(project.getUpdatedAt(), project.getCreatedAt());
        String title = firstNonBlank(project.getProjectName(), "Untitled project");
        User creator = project.getCreatedBy();
        String listedBy = firstRealName(
                ListingActivityService.displayName(creator),
                creator != null ? actorNames.get(creator.getId()) : null,
                personNameFromEvents(events, actorNames));

        List<LiveListingEditDto> edits = toEdits(
                events,
                actorNames,
                listedBy,
                project.getCreatedAt(),
                project.getUpdatedAt(),
                null,
                null,
                title);
        LiveListingEditDto latest = edits.isEmpty() ? null : edits.get(0);

        return new LiveListingRowDto(
                "PROJECT",
                "Website project",
                "ADMIN",
                (long) project.getId(),
                title,
                blankToNull(project.getProjectName()),
                builder,
                city,
                blankToNull(project.getProjectLocality()),
                type,
                null,
                blankToNull(project.getProjectConfiguration()),
                firstNonBlank(project.getProjectPrice(), "—"),
                firstNonBlank(statusName, "Live"),
                listedBy,
                creator != null ? blankToNull(creator.getEmail()) : null,
                project.getCreatedAt(),
                wentLive,
                slug != null ? "/" + slug : null,
                "/admin/dashboard/manage-projects",
                latest != null ? latest.actorName() : listedBy,
                latest != null ? latest.occurredAt() : wentLive,
                latest != null ? summaryOf(latest, title) : null,
                edits
        );
    }

    private LiveListingRowDto toPortalRow(
            PropertyListing listing,
            List<ListingActivityEvent> events,
            Map<Integer, String> actorNames) {
        User user = listing.getUser();
        String listerType = user != null ? userRoleService.resolvePortalPersonaLabel(user) : "BROKER";
        String listedBy = firstRealName(
                ListingActivityService.displayName(user),
                user != null ? actorNames.get(user.getId()) : null,
                personNameFromEvents(events, actorNames));
        String listedByEmail = user != null ? blankToNull(user.getEmail()) : null;
        City city = listing.getCity();
        String title = firstNonBlank(listing.getTitle(), listing.getProjectName(), buildFallbackTitle(listing));
        LocalDateTime wentLive = firstNonNull(listing.getApprovedAt(), listing.getUpdatedAt(), listing.getCreatedAt());
        String sourceLabel = "OWNER".equals(listerType) ? "Owner portal" : "Broker portal";

        List<LiveListingEditDto> edits = toEdits(
                events,
                actorNames,
                listedBy,
                listing.getCreatedAt(),
                listing.getUpdatedAt(),
                listing.getApprovedBy(),
                listing.getApprovedAt(),
                title);
        LiveListingEditDto latest = edits.isEmpty() ? null : edits.get(0);

        return new LiveListingRowDto(
                "PORTAL",
                sourceLabel,
                listerType,
                listing.getId(),
                title,
                blankToNull(listing.getProjectName()),
                blankToNull(listing.getBuilderName()),
                city != null ? blankToNull(city.getName()) : null,
                blankToNull(listing.getLocalityName()),
                firstNonBlank(listing.getListingType(), listing.getSubType()),
                blankToNull(listing.getTransaction()),
                configurationOf(listing),
                formatPrice(listing.getTotalPrice()),
                "Live",
                listedBy,
                listedByEmail,
                listing.getCreatedAt(),
                wentLive,
                "/properties/" + slugify(title, listing.getId()),
                "/admin/dashboard/property-approvals/" + listing.getId(),
                latest != null ? latest.actorName() : listedBy,
                latest != null ? latest.occurredAt() : wentLive,
                latest != null ? summaryOf(latest, title) : null,
                edits
        );
    }

    private Map<String, List<ListingActivityEvent>> loadActivity(List<Long> projectIds, List<Long> portalIds) {
        Map<String, List<ListingActivityEvent>> byKey = new HashMap<>();
        for (ListingActivityEvent event : loadEvents(ListingActivityService.SOURCE_PROJECT, projectIds)) {
            byKey.computeIfAbsent(key(event.getSource(), event.getEntityId()), ignored -> new ArrayList<>()).add(event);
        }
        for (ListingActivityEvent event : loadEvents(ListingActivityService.SOURCE_PORTAL, portalIds)) {
            byKey.computeIfAbsent(key(event.getSource(), event.getEntityId()), ignored -> new ArrayList<>()).add(event);
        }
        return byKey;
    }

    private Map<Integer, String> loadActorNames(
            Map<String, List<ListingActivityEvent>> activity,
            List<Project> projects,
            List<PropertyListing> listings) {
        Set<Integer> ids = new HashSet<>();
        for (List<ListingActivityEvent> events : activity.values()) {
            for (ListingActivityEvent event : events) {
                if (event.getActorUserId() != null) {
                    ids.add(event.getActorUserId());
                }
            }
        }
        for (Project project : projects) {
            if (project.getCreatedBy() != null && project.getCreatedBy().getId() != null) {
                ids.add(project.getCreatedBy().getId());
            }
        }
        for (PropertyListing listing : listings) {
            if (listing.getUser() != null && listing.getUser().getId() != null) {
                ids.add(listing.getUser().getId());
            }
            if (listing.getApprovedBy() != null && listing.getApprovedBy().getId() != null) {
                ids.add(listing.getApprovedBy().getId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> names = new HashMap<>();
        for (User user : userRepository.findAllById(ids)) {
            String name = ListingActivityService.displayName(user);
            if (!ListingActivityService.isGenericActorName(name)) {
                names.put(user.getId(), name);
            }
        }
        return names;
    }

    private List<ListingActivityEvent> loadEvents(String source, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> all = new ArrayList<>(ids);
        List<ListingActivityEvent> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i += 400) {
            List<Long> batch = all.subList(i, Math.min(i + 400, all.size()));
            out.addAll(listingActivityEventRepository.findBySourceAndEntityIdIn(source, batch));
        }
        return out;
    }

    private List<LiveListingEditDto> toEdits(
            List<ListingActivityEvent> stored,
            Map<Integer, String> actorNames,
            String fallbackName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            User approver,
            LocalDateTime approvedAt,
            String title) {
        List<LiveListingEditDto> out = new ArrayList<>();
        if (stored != null && !stored.isEmpty()) {
            stored.stream()
                    .sorted(Comparator.comparing(
                            ListingActivityEvent::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(MAX_EDITS_PER_ROW)
                    .map(event -> toEditDto(event, actorNames))
                    .forEach(out::add);
            return out;
        }

        String knownName = firstRealName(fallbackName);
        if (updatedAt != null && createdAt != null && updatedAt.isAfter(createdAt.plusMinutes(1))) {
            out.add(new LiveListingEditDto(
                    firstNonBlank(knownName, "Unknown"),
                    ListingActivityService.ACTION_UPDATED,
                    actionLabel(ListingActivityService.ACTION_UPDATED),
                    blankToNull(title),
                    updatedAt));
        }
        if (approvedAt != null) {
            String approverName = firstRealName(
                    ListingActivityService.displayName(approver),
                    approver != null ? actorNames.get(approver.getId()) : null);
            out.add(new LiveListingEditDto(
                    firstNonBlank(approverName, "Unknown"),
                    ListingActivityService.ACTION_APPROVED,
                    actionLabel(ListingActivityService.ACTION_APPROVED),
                    "Approved and made live",
                    approvedAt));
        }
        if (createdAt != null) {
            out.add(new LiveListingEditDto(
                    firstNonBlank(knownName, "Unknown"),
                    ListingActivityService.ACTION_CREATED,
                    actionLabel(ListingActivityService.ACTION_CREATED),
                    blankToNull(title),
                    createdAt));
        }
        out.sort(Comparator.comparing(
                LiveListingEditDto::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())));
        if (out.size() > MAX_EDITS_PER_ROW) {
            return new ArrayList<>(out.subList(0, MAX_EDITS_PER_ROW));
        }
        return out;
    }

    private LiveListingEditDto toEditDto(ListingActivityEvent event, Map<Integer, String> actorNames) {
        return new LiveListingEditDto(
                firstNonBlank(resolveEventActor(event, actorNames), "Unknown"),
                event.getAction(),
                actionLabel(event.getAction()),
                blankToNull(event.getDetail()),
                event.getOccurredAt());
    }

    private static String personNameFromEvents(
            List<ListingActivityEvent> events,
            Map<Integer, String> actorNames) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        String createdBy = null;
        String any = null;
        List<ListingActivityEvent> ordered = events.stream()
                .sorted(Comparator.comparing(
                        ListingActivityEvent::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (ListingActivityEvent event : ordered) {
            String name = resolveEventActor(event, actorNames);
            if (ListingActivityService.isGenericActorName(name)) {
                continue;
            }
            if (any == null) {
                any = name;
            }
            if (createdBy == null && ListingActivityService.ACTION_CREATED.equalsIgnoreCase(event.getAction())) {
                createdBy = name;
            }
        }
        return firstNonBlank(createdBy, any);
    }

    private static String resolveEventActor(ListingActivityEvent event, Map<Integer, String> actorNames) {
        if (event == null) {
            return null;
        }
        String stored = event.getActorName();
        String fromUser = event.getActorUserId() != null ? actorNames.get(event.getActorUserId()) : null;
        if (!ListingActivityService.isGenericActorName(fromUser)) {
            return fromUser;
        }
        if (!ListingActivityService.isGenericActorName(stored)) {
            return stored.trim();
        }
        return null;
    }

    private static String firstRealName(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!ListingActivityService.isGenericActorName(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String summaryOf(LiveListingEditDto edit, String title) {
        String label = firstNonBlank(edit.actionLabel(), "Updated");
        String detail = blankToNull(edit.detail());
        if (detail == null) {
            return label;
        }
        if (title != null && detail.equalsIgnoreCase(title.trim())) {
            return label;
        }
        return label + " · " + detail;
    }

    private static String actionLabel(String action) {
        if (action == null) {
            return "Activity";
        }
        return switch (action.toUpperCase(Locale.ROOT)) {
            case ListingActivityService.ACTION_CREATED -> "Created";
            case ListingActivityService.ACTION_UPDATED -> "Updated";
            case ListingActivityService.ACTION_STATUS_CHANGED -> "Status changed";
            case ListingActivityService.ACTION_APPROVED -> "Approved";
            case ListingActivityService.ACTION_REJECTED -> "Rejected";
            case ListingActivityService.ACTION_PUBLISHED -> "Published";
            case ListingActivityService.ACTION_UNPUBLISHED -> "Unpublished";
            default -> action.replace('_', ' ');
        };
    }

    private static String key(String source, Long entityId) {
        return String.valueOf(source).toUpperCase(Locale.ROOT) + ":" + entityId;
    }

    private static String configurationOf(PropertyListing listing) {
        if (listing.getBedrooms() != null && listing.getBedrooms() > 0) {
            int beds = listing.getBedrooms();
            return beds + " BHK";
        }
        return blankToNull(listing.getSubType());
    }

    private static String buildFallbackTitle(PropertyListing listing) {
        String type = firstNonBlank(listing.getSubType(), listing.getListingType(), "Property");
        if (listing.getBedrooms() != null && listing.getBedrooms() > 0) {
            return listing.getBedrooms() + " BHK " + type;
        }
        return type;
    }

    private static String formatPrice(Double price) {
        if (price == null) return "—";
        if (price >= 10_000_000d) {
            return "₹" + trimZeros(price / 10_000_000d) + " Cr";
        }
        if (price >= 100_000d) {
            return "₹" + trimZeros(price / 100_000d) + " L";
        }
        return "₹" + INR.format(Math.round(price));
    }

    private static String trimZeros(double value) {
        String formatted = String.format(Locale.US, "%.2f", value);
        if (formatted.endsWith(".00")) {
            return formatted.substring(0, formatted.length() - 3);
        }
        if (formatted.endsWith("0")) {
            return formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private static String slugify(String title, Long id) {
        String base = title == null ? "" : title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (base.isBlank()) return String.valueOf(id);
        return base + "-" + id;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) return null;
        for (T value : values) {
            if (value != null) return value;
        }
        return null;
    }
}
