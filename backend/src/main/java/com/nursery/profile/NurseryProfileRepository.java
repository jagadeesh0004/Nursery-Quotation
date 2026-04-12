package com.nursery.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NurseryProfileRepository extends JpaRepository<NurseryProfile, UUID> {
    Optional<NurseryProfile> findByUserId(UUID userId);
}
