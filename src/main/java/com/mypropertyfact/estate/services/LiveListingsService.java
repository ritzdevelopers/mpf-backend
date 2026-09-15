package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.LiveListingRowDto;
import com.mypropertyfact.estate.dtos.LiveListingSummaryDto;
import com.mypropertyfact.estate.dtos.LiveListingsResponse;
import com.mypropertyfact.estate.entities.City;
import com.mypropertyfact.estate.entities.Project;
import com.mypropertyfact.estate.entities.PropertyListing;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.enums.ProjectApprovalStatus;
import com.mypropertyfact.estate.repositories.ProjectRepository;
import com.mypropertyfact.estate.repositories.PropertyListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LiveListingsService {

    private static final NumberFormat INR = NumberFormat.getInstance(new Locale("en", "IN"));

    private final ProjectRepository projectRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final UserRoleService userRoleService;

    @Transactional(readOnly = true)
    public LiveListingsResponse build() {
        List<LiveListingRowDto> rows = new ArrayList<>();

        for (Project project : projectRepository.findLiveWithDetails()) {
            rows.add(toProjectRow(project));
        }

        long brokerListings = 0;
        long ownerListings = 0;
        for (PropertyListing listing : propertyListingRepository
                .findByApprovalStatusWithUserAndCity(ProjectApprovalStatus.APPROVED)) {
            LiveListingRowDto row = toPortalRow(listing);
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

    private LiveListingRowDto toProjectRow(Project project) {
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

        return new LiveListingRowDto(
                "PROJECT",
                "Website project",
                "ADMIN",
                (long) project.getId(),
                firstNonBlank(project.getProjectName(), "Untitled project"),
                blankToNull(project.getProjectName()),
                builder,
                city,
                blankToNull(project.getProjectLocality()),
                type,
                null,
                blankToNull(project.getProjectConfiguration()),
                firstNonBlank(project.getProjectPrice(), "—"),
                firstNonBlank(statusName, "Live"),
                "Admin",
                null,
                project.getCreatedAt(),
                wentLive,
                slug != null ? "/" + slug : null,
                "/admin/dashboard/manage-projects"
        );
    }

    private LiveListingRowDto toPortalRow(PropertyListing listing) {
        User user = listing.getUser();
        String listerType = user != null ? userRoleService.resolvePortalPersonaLabel(user) : "BROKER";
        String listedBy = user != null
                ? firstNonBlank(user.getFullName(), user.getEmail(), "Portal user")
                : "Portal user";
        String listedByEmail = user != null ? blankToNull(user.getEmail()) : null;
        City city = listing.getCity();
        String title = firstNonBlank(listing.getTitle(), listing.getProjectName(), buildFallbackTitle(listing));
        LocalDateTime wentLive = firstNonNull(listing.getApprovedAt(), listing.getUpdatedAt(), listing.getCreatedAt());
        String sourceLabel = "OWNER".equals(listerType) ? "Owner portal" : "Broker portal";

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
                "/admin/dashboard/property-approvals/" + listing.getId()
        );
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
