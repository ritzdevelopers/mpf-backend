package com.mypropertyfact.estate.specs;

import com.mypropertyfact.estate.entities.AdminAuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
}
