package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.Enquery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EnqueryRepository extends JpaRepository<Enquery, Integer> {
    List<Enquery> findByPropertyId(Long propertyId);
    List<Enquery> findByPropertyIdIn(List<Long> propertyIds);
    List<Enquery> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

    @Query("SELECT COUNT(e) FROM Enquery e WHERE e.propertyId IN (SELECT pl.id FROM PropertyListing pl WHERE pl.user.id = :userId)")
    long countByUserListings(@Param("userId") Integer userId);
}
