package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.dtos.PropertyListingImagesDto;
import com.mypropertyfact.estate.dtos.PropertyShortDetailsDto;
import com.mypropertyfact.estate.entities.PropertyListing;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.enums.ProjectApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyListingRepository extends JpaRepository<PropertyListing, Long> {

    // Find all listings by user
    List<PropertyListing> findByUser(User user);

    // Find all listings by user ID
    List<PropertyListing> findByUserId(Integer userId);

    // Find listing by ID and user (for ownership verification)
    Optional<PropertyListing> findByIdAndUserId(Long id, Integer userId);

    // Find listings by approval status
    List<PropertyListing> findByApprovalStatus(ProjectApprovalStatus status);

    // Find listings by user and approval status
    List<PropertyListing> findByUserIdAndApprovalStatus(Integer userId, ProjectApprovalStatus status);

    // Find approved listings
    List<PropertyListing> findByApprovalStatusOrderByCreatedAtDesc(ProjectApprovalStatus status);

    // Find listings by city
    List<PropertyListing> findByCityId(Integer cityId);

    // Find listings by builder
    List<PropertyListing> findByBuilderId(Integer builderId);

    // Find listings by listing type (Residential/Commercial)
    List<PropertyListing> findByListingType(String listingType);

    // Find listings by transaction type (Sale/Rent)
    List<PropertyListing> findByTransaction(String transaction);

    // Search listings by title
    // Note: Description search removed due to CLOB type incompatibility with
    // LOWER() function
    @Query("SELECT pl FROM PropertyListing pl WHERE " +
            "LOWER(pl.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PropertyListing> searchByKeyword(@Param("keyword") String keyword);

    // Find listings with pagination
    Page<PropertyListing> findByApprovalStatus(ProjectApprovalStatus status, Pageable pageable);

    // Count listings by user
    long countByUserId(Integer userId);

    // Count listings by approval status
    long countByApprovalStatus(ProjectApprovalStatus status);

    @Query("""
            SELECT new com.mypropertyfact.estate.dtos.PropertyShortDetailsDto(
                pl.id,
                pl.listingType,
                pl.transaction,
                pl.subType,
                pl.status,
                pl.projectName,
                pl.builderName,
                pl.address,
                pl.localityName,
                pl.city.name,
                pl.pincode,
                pl.carpetArea,
                pl.builtUpArea,
                pl.totalPrice,
                pl.pricePerSqft,
                pl.bathrooms,
                pl.bedrooms,
                pl.facing,
                pl.createdAt
            )
            FROM PropertyListing pl
            WHERE pl.approvalStatus = :status
            ORDER BY pl.createdAt DESC
            """)
    List<PropertyShortDetailsDto> getPropertyShortDetails(@Param("status") ProjectApprovalStatus status);

    @Query("""
            SELECT new com.mypropertyfact.estate.dtos.PropertyListingImagesDto(
                pi.id,
                pi.imageUrl,
                pi.imageName
            ) FROM PropertyListingImage pi WHERE pi.propertyListing.id = :propertyListingId
            ORDER BY pi.displayOrder ASC
            """)
    List<PropertyListingImagesDto> findByPropertyListingId(Long propertyListingId);
}
