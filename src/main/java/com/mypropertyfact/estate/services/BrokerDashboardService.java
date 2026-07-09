package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.BrokerDashboardStatsResponse;
import com.mypropertyfact.estate.enums.ProjectApprovalStatus;
import com.mypropertyfact.estate.repositories.EnqueryRepository;
import com.mypropertyfact.estate.repositories.PropertyListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class BrokerDashboardService {

    private final PropertyListingRepository propertyListingRepository;
    private final EnqueryRepository enqueryRepository;

    public BrokerDashboardStatsResponse getStatsForUser(Integer userId) {
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();

        long totalListings = propertyListingRepository.countByUserId(userId);
        long liveListings = propertyListingRepository.countByUserIdAndApprovalStatus(
                userId, ProjectApprovalStatus.APPROVED);
        long pendingListings = propertyListingRepository.countByUserIdAndApprovalStatus(
                userId, ProjectApprovalStatus.PENDING);
        long draftListings = propertyListingRepository.countByUserIdAndApprovalStatus(
                userId, ProjectApprovalStatus.DRAFT);
        long rejectedListings = propertyListingRepository.countByUserIdAndApprovalStatus(
                userId, ProjectApprovalStatus.REJECTED);
        long enquiryCount = enqueryRepository.countByUserListings(userId);
        long addedThisMonth = propertyListingRepository.countByUserIdAndCreatedAtAfter(userId, monthStart);
        long cityCount = propertyListingRepository.countDistinctCitiesByUserId(userId);
        long builderCount = propertyListingRepository.countDistinctBuildersByUserId(userId);
        long amenityCount = propertyListingRepository.countDistinctAmenitiesByUserId(userId);
        long propertyTypeCount = propertyListingRepository.countDistinctPropertyTypesByUserId(userId);

        return new BrokerDashboardStatsResponse(
                totalListings,
                liveListings,
                pendingListings,
                draftListings,
                rejectedListings,
                enquiryCount,
                addedThisMonth,
                cityCount,
                builderCount,
                amenityCount,
                propertyTypeCount
        );
    }
}
