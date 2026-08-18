package com.mypropertyfact.estate.specs;

import com.mypropertyfact.estate.entities.AdminAuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AdminAuditLogSpecs {

    private AdminAuditLogSpecs() {
    }

    public static Specification<AdminAuditLog> occurredAtFrom(LocalDateTime from) {
        return (root, query, cb) -> {
            if (from == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
        };
    }

    public static Specification<AdminAuditLog> occurredAtTo(LocalDateTime to) {
        return (root, query, cb) -> {
            if (to == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("occurredAt"), to);
        };
    }

    public static Specification<AdminAuditLog> actorEmailContains(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + email.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("actorEmail")), pattern);
        };
    }

    public static Specification<AdminAuditLog> successEqual(Boolean success) {
        return (root, query, cb) -> {
            if (success == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("success"), success);
        };
    }

    public static Specification<AdminAuditLog> pathTaskOrClientContains(String fragment) {
        return (root, query, cb) -> {
            if (fragment == null || fragment.isBlank()) {
                return cb.conjunction();
            }
            List<Predicate> parts = new ArrayList<>();
            String f = "%" + fragment.trim().toLowerCase() + "%";
            parts.add(cb.like(cb.lower(root.get("requestPath")), f));
            parts.add(cb.like(cb.lower(cb.coalesce(root.get("queryString"), "")), f));
            parts.add(cb.like(cb.lower(cb.coalesce(root.get("taskLabel"), "")), f));
            parts.add(cb.like(cb.lower(cb.coalesce(root.get("clientAdminPage"), "")), f));
            return cb.or(parts.toArray(new Predicate[0]));
        };
    }

    /**
     * Free-text match on email, task, path, admin page, and (optional) actor user ids
     * whose full name matched the query.
     */
    public static Specification<AdminAuditLog> textQuery(String query, Collection<Integer> matchingUserIds) {
        return (root, queryObj, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }
            String f = "%" + query.trim().toLowerCase() + "%";
            List<Predicate> parts = new ArrayList<>();
            parts.add(cb.like(cb.lower(root.get("actorEmail")), f));
            parts.add(cb.like(cb.lower(cb.coalesce(root.get("taskLabel"), "")), f));
            parts.add(cb.like(cb.lower(root.get("requestPath")), f));
            parts.add(cb.like(cb.lower(cb.coalesce(root.get("queryString"), "")), f));
            parts.add(cb.like(cb.lower(cb.coalesce(root.get("clientAdminPage"), "")), f));
            if (matchingUserIds != null && !matchingUserIds.isEmpty()) {
                parts.add(root.get("actorUserId").in(matchingUserIds));
            }
            return cb.or(parts.toArray(new Predicate[0]));
        };
    }

    /**
     * view / change / delete / auth — same buckets the activity-log UI uses.
     */
    public static Specification<AdminAuditLog> kind(String kind) {
        return (root, query, cb) -> {
            if (kind == null || kind.isBlank()) {
                return cb.conjunction();
            }
            String k = kind.trim().toLowerCase();
            var method = cb.upper(cb.coalesce(root.get("httpMethod"), ""));
            var task = cb.lower(cb.coalesce(root.get("taskLabel"), ""));
            var path = cb.lower(cb.coalesce(root.get("requestPath"), ""));
            Predicate isDelete = cb.or(
                    cb.equal(method, "DELETE"),
                    cb.like(task, "%delete%"),
                    cb.like(task, "%remove%"),
                    cb.like(task, "%purge%"));
            Predicate isAuth = cb.or(
                    cb.like(task, "%login%"),
                    cb.like(task, "%logout%"),
                    cb.like(task, "%session%"),
                    cb.like(task, "%sign in%"),
                    cb.like(task, "%sign out%"),
                    cb.like(path, "%/login%"),
                    cb.like(path, "%/logout%"),
                    cb.like(path, "%/auth/%"));
            return switch (k) {
                case "delete" -> isDelete;
                case "auth" -> isAuth;
                case "change" -> cb.and(
                        cb.or(
                                cb.equal(method, "POST"),
                                cb.equal(method, "PUT"),
                                cb.equal(method, "PATCH")),
                        cb.not(isDelete),
                        cb.not(isAuth));
                case "view" -> cb.and(cb.equal(method, "GET"), cb.not(isAuth));
                default -> cb.conjunction();
            };
        };
    }
}
