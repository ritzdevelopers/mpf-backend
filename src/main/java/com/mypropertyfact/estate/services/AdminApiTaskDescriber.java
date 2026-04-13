package com.mypropertyfact.estate.services;

import org.springframework.http.HttpMethod;

/**
 * Maps admin API method + path to a short human-readable task label for audit logs.
 */
public final class AdminApiTaskDescriber {

    private AdminApiTaskDescriber() {
    }

    public static String describe(String httpMethod, String requestPath) {
        if (requestPath == null) {
            return "Admin action";
        }
        String path = requestPath;
        String method = httpMethod != null ? httpMethod.toUpperCase() : "";

        if (path.contains("/dashboard-stats")) {
            return "View dashboard statistics";
        }
        if (path.contains("/dashboard/my-activity")) {
            return "View recent dashboard activity";
        }
        if (path.contains("/dashboard/site-traffic-trends")) {
            return "View website traffic trend chart";
        }
        if (path.contains("/dashboard/site-traffic-live")) {
            return "View live website traffic chart";
        }
        if (path.contains("/dashboard/site-traffic-today")) {
            return "View today 24h website traffic";
        }
        if (path.contains("/api/v1/admin/property-listings")) {
            if (HttpMethod.POST.matches(method)) {
                return "Submit or update property listing (admin)";
            }
            if (path.matches(".*/property-listings/\\d+.*")) {
                return HttpMethod.GET.matches(method)
                        ? "Open property listing details"
                        : "Update property listing status or details";
            }
            return "Browse or load property listings";
        }
        if (path.contains("/api/v1/admin/roles")) {
            if (HttpMethod.GET.matches(method)) {
                return path.matches(".*/roles/\\d+.*") ? "View role details" : "List roles";
            }
            if (HttpMethod.POST.matches(method)) {
                return "Create or update role";
            }
            if (HttpMethod.PUT.matches(method) || HttpMethod.PATCH.matches(method)) {
                return "Update role";
            }
            if (HttpMethod.DELETE.matches(method)) {
                return "Delete role";
            }
            return "Role management";
        }
        if (path.contains("/api/v1/admin/super/traffic")) {
            return "View live traffic summary";
        }
        if (path.contains("/api/v1/admin/super/audit-logs")) {
            return "View admin audit logs";
        }

        return fallbackVerb(method, path);
    }

    private static String fallbackVerb(String method, String path) {
        int last = path.lastIndexOf('/');
        String tail = last >= 0 && last < path.length() - 1 ? path.substring(last + 1) : path;
        tail = tail.replaceAll("[?#].*$", "");
        if (tail.isBlank()) {
            tail = "resource";
        }
        String title = slugToTitle(tail);
        if (HttpMethod.GET.matches(method)) {
            return "View " + title;
        }
        if (HttpMethod.POST.matches(method)) {
            return "Create or submit " + title;
        }
        if (HttpMethod.PUT.matches(method) || HttpMethod.PATCH.matches(method)) {
            return "Update " + title;
        }
        if (HttpMethod.DELETE.matches(method)) {
            return "Delete " + title;
        }
        return method + " " + title;
    }

    private static String slugToTitle(String slug) {
        String s = slug.replace('-', ' ').replace('_', ' ').trim();
        if (s.isEmpty()) {
            return "item";
        }
        String[] parts = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
