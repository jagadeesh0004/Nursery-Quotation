package com.nursery.plant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DimensionRepository extends JpaRepository<Dimension, Long> {

    Optional<Dimension> findByPlantIdAndDimension(Long plantId, String dimension);

    boolean existsByPlantIdAndDimension(Long plantId, String dimension);
}
