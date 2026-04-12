package com.nursery.plant;

import java.util.List;

/**
 * Helper record for storing plant master data during initialization.
 * Used by PlantService to populate default plants for new users.
 */
public record PlantInitializationData(
    String plantName,
    List<String> bagSizes
) {}

