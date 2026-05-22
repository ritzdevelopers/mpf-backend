package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Integer> {

    Optional<PendingRegistration> findByEmail(String email);

    void deleteByEmail(String email);
}
