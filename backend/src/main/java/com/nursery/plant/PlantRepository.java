package com.nursery.plant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlantRepository extends JpaRepository<Plant, Long> {

    List<Plant> findByUserIdOrderByNameAsc(UUID userId);

    Optional<Plant> findByUserIdAndNameNormalized(UUID userId, String nameNormalized);

    @Query("SELECT p FROM Plant p LEFT JOIN FETCH p.dimensions WHERE p.userId = :userId ORDER BY p.name ASC")
    List<Plant> findByUserIdWithDimensions(UUID userId);

    boolean existsByUserIdAndNameNormalized(UUID userId, String nameNormalized);
}
