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
    public static final String MANAGE_OPTIONS = "MANAGE_OPTIONS";
    public static final String MANAGE_PROJECTS = "MANAGE_PROJECTS";
    public static final String MANAGE_INSIGHTS = "MANAGE_INSIGHTS";
    public static final String MANAGE_BLOGS = "MANAGE_BLOGS";
    public static final String MANAGE_WEB_STORIES = "MANAGE_WEB_STORIES";
    public static final String MANAGE_AMENITIES = "MANAGE_AMENITIES";
    public static final String MANAGE_FEATURES = "MANAGE_FEATURES";
    public static final String MANAGE_NEARBY_BENEFITS = "MANAGE_NEARBY_BENEFITS";

    private static final List<Map<String, String>> DEFINITION_ROWS = List.of(
            entry(MANAGE_WEBSITE, "Manage website", "Home banners and similar site content"),
            entry(MANAGE_OPTIONS, "Manage options", "Countries, states, cities, builders, project types, careers, etc."),
            entry(MANAGE_PROJECTS, "Manage projects", "Projects, banners, galleries, FAQs, amenities, floor plans, Excel bulk upload"),
            entry(MANAGE_INSIGHTS, "Insight management", "City price data, locality scores, headers, insight categories, top developers"),
            entry(MANAGE_BLOGS, "Blog management", "Blogs and blog categories"),
            entry(MANAGE_WEB_STORIES, "Web story management", "Web stories and categories"),
            entry(MANAGE_AMENITIES, "Amenities", "Master amenities list"),
            entry(MANAGE_FEATURES, "Features", "Property features"),
            entry(MANAGE_NEARBY_BENEFITS, "Nearby benefits", "Location / nearby benefit content"));

    private static Map<String, String> entry(String key, String label, String description) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("description", description);
        return m;
    }

    public static List<Map<String, String>> definitions() {
        return DEFINITION_ROWS;
    }

    public static Set<String> allKeys() {
        return DEFINITION_ROWS.stream().map(r -> r.get("key")).collect(Collectors.toUnmodifiableSet());
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
