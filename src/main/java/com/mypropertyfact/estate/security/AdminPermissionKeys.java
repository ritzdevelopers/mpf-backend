package com.mypropertyfact.estate.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Granular capabilities assignable to {@code ADMIN} by {@code SUPERADMIN}.
 * Super Admin implicitly has all permissions.
 */
public final class AdminPermissionKeys {

    private AdminPermissionKeys() {}

    public static final String MANAGE_WEBSITE = "MANAGE_WEBSITE";
    public static final String MANAGE_LISTING_FAQS = "MANAGE_LISTING_FAQS";
    public static final String MANAGE_OPTIONS = "MANAGE_OPTIONS";
    public static final String MANAGE_PROJECTS = "MANAGE_PROJECTS";
    public static final String MANAGE_INSIGHTS = "MANAGE_INSIGHTS";
    public static final String MANAGE_BLOGS = "MANAGE_BLOGS";
    public static final String MANAGE_WEB_STORIES = "MANAGE_WEB_STORIES";
    public static final String MANAGE_AMENITIES = "MANAGE_AMENITIES";
    public static final String MANAGE_FEATURES = "MANAGE_FEATURES";
    public static final String MANAGE_NEARBY_BENEFITS = "MANAGE_NEARBY_BENEFITS";
    public static final String MANAGE_PROPERTY_APPROVALS = "MANAGE_PROPERTY_APPROVALS";
    public static final String MANAGE_ENQUIRIES = "MANAGE_ENQUIRIES";
    /** Pro: bulk-add FAQs across multiple listing pages in one request. */
    public static final String BULK_LISTING_FAQS = "BULK_LISTING_FAQS";

    private static final List<Map<String, String>> DEFINITION_ROWS = List.of(
            entry(MANAGE_WEBSITE, "Manage website", "Home banners and similar site content", false),
            entry(MANAGE_LISTING_FAQS, "Manage listing page FAQs", "FAQs for listing pages (city hubs, BHK, shops, food court, etc.)", false),
            entry(MANAGE_OPTIONS, "Manage options", "Countries, states, cities, builders, project types, careers, etc.", false),
            entry(MANAGE_PROJECTS, "Manage projects", "Projects, banners, galleries, FAQs, amenities, floor plans, Excel bulk upload", false),
            entry(MANAGE_INSIGHTS, "Insight management", "City price data, locality scores, headers, insight categories, top developers", false),
            entry(MANAGE_BLOGS, "Blog management", "Blogs and blog categories", false),
            entry(MANAGE_WEB_STORIES, "Web story management", "Web stories and categories", false),
            entry(MANAGE_AMENITIES, "Amenities", "Master amenities list", false),
            entry(MANAGE_FEATURES, "Features", "Property features", false),
            entry(MANAGE_NEARBY_BENEFITS, "Nearby benefits", "Location / nearby benefit content", false),
            entry(MANAGE_PROPERTY_APPROVALS, "Property approvals", "Review and approve user-submitted property listings", false),
            entry(MANAGE_ENQUIRIES, "Manage enquiries", "View leads and enquiries after entering the 4-digit code set by Super Admin", false),
            entry(BULK_LISTING_FAQS, "Bulk FAQ add", "Add multiple FAQs for various listing pages in one go", true));

    private static Map<String, String> entry(String key, String label, String description, boolean pro) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("description", description);
        m.put("pro", pro ? "true" : "false");
        return m;
    }

    public static List<Map<String, String>> definitions() {
        return DEFINITION_ROWS;
    }

    public static Set<String> allKeys() {
        return DEFINITION_ROWS.stream().map(r -> r.get("key")).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Permissions that must be granted explicitly (never in default / legacy full CMS access).
     */
    public static boolean isExplicitGrantOnly(String key) {
        return MANAGE_ENQUIRIES.equals(key) || BULK_LISTING_FAQS.equals(key);
    }

    /**
     * Default CMS permissions for staff Admin (all modules except explicit-grant / pro features).
     * Enquiries and pro features must be granted explicitly by Super Admin.
     */
    public static Set<String> defaultStaffAdminKeys() {
        return DEFINITION_ROWS.stream()
                .map(r -> r.get("key"))
                .filter(k -> !isExplicitGrantOnly(k))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the canonical uppercase key from {@link #allKeys()}, or null if unknown.
     */
    public static String canonicalizePermissionKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        for (String k : allKeys()) {
            if (k.equalsIgnoreCase(t)) {
                return k;
            }
        }
        return null;
    }
}
