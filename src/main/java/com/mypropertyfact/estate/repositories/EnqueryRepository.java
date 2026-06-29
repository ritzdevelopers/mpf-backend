package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.Enquery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EnqueryRepository extends JpaRepository<Enquery, Integer> {
    List<Enquery> findByPropertyId(Long propertyId);
    List<Enquery> findByPropertyIdIn(List<Long> propertyIds);
    List<Enquery> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);
}
