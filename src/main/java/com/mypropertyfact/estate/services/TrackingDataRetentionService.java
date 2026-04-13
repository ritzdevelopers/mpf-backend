package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.repositories.AdminAuditLogRepository;
import com.mypropertyfact.estate.repositories.SiteTrafficEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingDataRetentionService {

    private final SiteTrafficEventRepository siteTrafficEventRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    @Value("${mpf.tracking.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldRows() {
        int days = Math.max(1, retentionDays);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int t = siteTrafficEventRepository.deleteByOccurredAtBefore(cutoff);
        int a = adminAuditLogRepository.deleteByOccurredAtBefore(cutoff);
        if (t > 0 || a > 0) {
            log.info("Tracking retention purge: site_traffic_event={}, admin_audit_log={}, cutoff={}", t, a, cutoff);
        }
    }
}
