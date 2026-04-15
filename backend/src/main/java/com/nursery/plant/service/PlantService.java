package com.nursery.plant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursery.common.exception.BusinessException;
import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.plant.*;
import com.nursery.plant.dto.PlantDtos.*;
import com.nursery.plant.mapper.PlantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PlantRepository plantRepository;
    private final DimensionRepository dimensionRepository;
    private final PlantMapper plantMapper;
    private final JdbcTemplate jdbcTemplate;

    // ...existing code...

    @Transactional(readOnly = true)
    public List<PlantResponse> getAllPlants(UUID userId) {
        return plantRepository.findByUserIdWithDimensions(userId)
                .stream()
                .map(plantMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlantResponse getPlant(UUID userId, Long plantId) {
        Plant plant = findOwnedPlant(userId, plantId);
        return plantMapper.toResponse(plant);
    }

    // ─── Catalog Create ──────────────────────────────────────────────

    @Transactional
    public PlantResponse createPlant(UUID userId, CreatePlantRequest request) {
        String normalized = normalize(request.name());

        if (plantRepository.existsByUserIdAndNameNormalized(userId, normalized)) {
            throw new BusinessException("A plant with this name already exists in your catalog.");
        }

        Plant plant = Plant.builder()
                .userId(userId)
                .name(request.name().trim())
                .nameNormalized(normalized)
                .build();

        Plant saved = plantRepository.save(plant);
        log.info("Plant created: {} for userId: {}", saved.getName(), userId);
        return plantMapper.toResponse(saved);
    }

    @Transactional
    public PlantResponse addDimension(UUID userId, Long plantId, AddDimensionRequest request) {
        Plant plant = findOwnedPlant(userId, plantId);
        String dim = normalizeDimension(request.dimension());

        if (dimensionRepository.existsByPlantIdAndDimension(plantId, dim)) {
            throw new BusinessException("Dimension '" + dim + "' already exists for this plant.");
        }

        Dimension dimension = Dimension.builder()
                .plant(plant)
                .dimension(dim)
                .price(request.price() != null ? request.price() : java.math.BigDecimal.ZERO)
                .build();

        dimensionRepository.save(dimension);
        plant.getDimensions().add(dimension);
        return plantMapper.toResponse(plant);
    }

    // ─── Catalog Delete (Soft) ───────────────────────────────────────

    @Transactional
    public void deletePlant(UUID userId, Long plantId) {
        Plant plant = findOwnedPlant(userId, plantId);
        plant.setDeletedAt(OffsetDateTime.now());
        plantRepository.save(plant);
        log.info("Plant soft-deleted: {} for userId: {}", plantId, userId);
    }

    @Transactional
    public void deleteDimension(UUID userId, Long plantId, Long dimensionId) {
        findOwnedPlant(userId, plantId); // ownership check
        Dimension dimension = dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> new ResourceNotFoundException("Dimension", dimensionId));
        if (!dimension.getPlant().getId().equals(plantId)) {
            throw new BusinessException("Dimension does not belong to this plant.");
        }
        dimension.setDeletedAt(OffsetDateTime.now());
        dimensionRepository.save(dimension);
    }

    // ─── Dynamic Creation (used by Quotation workflow) ───────────────

    /**
     * Finds or creates a plant by name for the given user.
     * Also finds or creates the specified dimension.
     * Called automatically during quotation item creation.
     */
    @Transactional
    public void findOrCreatePlantWithDimension(UUID userId, String plantName, String dimension) {
        // CRITICAL: Ensure user exists in DB before inserting plants
        // This prevents FK constraint violations when user ID comes from JWT
        ensureUserExists(userId);

        String normalizedName = normalize(plantName);
        String normalizedDim  = normalizeDimension(dimension);

        Plant plant = plantRepository.findByUserIdAndNameNormalized(userId, normalizedName)
                .orElseGet(() -> {
                    Plant newPlant = Plant.builder()
                            .userId(userId)
                            .name(plantName.trim())
                            .nameNormalized(normalizedName)
                            .build();
                    return plantRepository.save(newPlant);
                });

        if (!dimensionRepository.existsByPlantIdAndDimension(plant.getId(), normalizedDim)) {
            Dimension dim = Dimension.builder()
                    .plant(plant)
                    .dimension(normalizedDim)
                    .price(java.math.BigDecimal.ZERO)
                    .build();
            dimensionRepository.save(dim);
        }
    }

    /**
     * Initializes default plants for a new user with zero prices.
     * Called when user creates their account/profile.
     */
    @Transactional
    public void initializeDefaultPlantsForUser(UUID userId) {
        log.info("Initializing default plants for userId: {}", userId);
        
        // Ensure user exists in DB before adding plants
        ensureUserExists(userId);

        List<PlantInitializationData> defaultPlants = getDefaultPlantsData();

        for (PlantInitializationData plantData : defaultPlants) {
            String normalizedName = normalize(plantData.plantName());

            // Check if plant already exists
            Plant plant = plantRepository.findByUserIdAndNameNormalized(userId, normalizedName)
                    .orElseGet(() -> {
                        Plant newPlant = Plant.builder()
                                .userId(userId)
                                .name(plantData.plantName())
                                .nameNormalized(normalizedName)
                                .build();
                        return plantRepository.save(newPlant);
                    });

            // Add dimensions with zero price
            for (String bagSize : plantData.bagSizes()) {
                String normalizedDim = normalizeDimension(bagSize);

                if (!dimensionRepository.existsByPlantIdAndDimension(plant.getId(), normalizedDim)) {
                    Dimension dimension = Dimension.builder()
                            .plant(plant)
                            .dimension(normalizedDim)
                            .price(java.math.BigDecimal.ZERO)
                            .build();
                    dimensionRepository.save(dimension);
                }
            }
        }

        log.info("Default plants initialized for userId: {}", userId);
    }

    /**
     * ✅ ASYNC version of initializeDefaultPlantsForUser.
     * Runs in background thread, doesn't block the profile save response.
     * Called from NurseryProfileService when a new profile is created.
     *
     * ⚠️ NOTE: Includes retry logic with delay to handle race conditions
     * where the user might not be committed to DB yet.
     */
    @Async
    public void initializeDefaultPlantsForUserAsync(UUID userId) {
        try {
            log.info("🌱 [ASYNC] Starting plant initialization for userId: {}", userId);

            // Retry logic: try up to 3 times with delays to handle FK constraint issues
            int maxRetries = 3;
            int retryCount = 0;
            Exception lastException = null;

            while (retryCount < maxRetries) {
                try {
                    initializeDefaultPlantsForUser(userId);
                    log.info("🌱 [ASYNC] Plant initialization completed for userId: {}", userId);
                    return; // Success
                } catch (Exception e) {
                    lastException = e;
                    retryCount++;

                    if (retryCount < maxRetries) {
                        long delayMs = (long) Math.pow(2, retryCount - 1) * 1000; // 1s, 2s, 4s
                        log.warn("⚠️ [ASYNC] Plant initialization failed (attempt {}/{}), retrying in {}ms. Error: {}",
                                 retryCount, maxRetries, delayMs, e.getMessage());
                        Thread.sleep(delayMs);
                    }
                }
            }

            // All retries failed
            log.error("❌ [ASYNC] Plant initialization failed after {} attempts for userId: {}",
                     maxRetries, userId, lastException);
        } catch (InterruptedException e) {
            log.error("❌ [ASYNC] Plant initialization interrupted for userId: {}", userId, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns the default plants master data.
     * This data is loaded from the plants.json file in the project root.
     */
    private List<PlantInitializationData> getDefaultPlantsData() {
        List<PlantInitializationData> fromResource = loadDefaultPlantsFromResource();
        if (!fromResource.isEmpty()) {
            return fromResource;
        }

        return List.of(
            new PlantInitializationData("Neem", List.of("8x10", "13x13", "21x21")),
            new PlantInitializationData("Cassia fistula", List.of("8x10", "21x21")),
            new PlantInitializationData("Cassia siamea", List.of("21x21")),
            new PlantInitializationData("Cassia javanica", List.of("21x21")),
            new PlantInitializationData("Custard Apple", List.of("8x10", "13x13", "21x21")),
            new PlantInitializationData("Papaya", List.of("8x10")),
            new PlantInitializationData("Guava", List.of("13x13")),
            new PlantInitializationData("Banana", List.of("8x10")),
            new PlantInitializationData("Pomegranate", List.of("8x10")),
            new PlantInitializationData("Mango", List.of()),
            new PlantInitializationData("Amla", List.of("13x13")),
            new PlantInitializationData("Shikakai", List.of()),
            new PlantInitializationData("Indian Plum (Ber)", List.of("13x13")),
            new PlantInitializationData("Pear Tree", List.of("13x13")),
            new PlantInitializationData("Coconut Tree", List.of()),
            new PlantInitializationData("Jackfruit Tree", List.of()),
            new PlantInitializationData("Areca Palm", List.of("8x10", "12x13", "15x16", "21x21")),
            new PlantInitializationData("Foxtail Palm", List.of("13x13", "18x18", "21x21")),
            new PlantInitializationData("Royal Palm", List.of("21x21")),
            new PlantInitializationData("Veitchia merrillii", List.of("21x21")),
            new PlantInitializationData("Raphis Palm", List.of("13x13")),
            new PlantInitializationData("Ficus Panda", List.of("18x18", "21x21")),
            new PlantInitializationData("Ficus lyrata", List.of("30x30")),
            new PlantInitializationData("Ficus religiosa (Peepal)", List.of("21x21")),
            new PlantInitializationData("Ficus benghalensis (Banyan)", List.of("21x21")),
            new PlantInitializationData("Ficus benjamina", List.of("25x25")),
            new PlantInitializationData("Banyan Tree", List.of("21x21")),
            new PlantInitializationData("Peepal Tree", List.of("21x21")),
            new PlantInitializationData("Arjuna Tree", List.of("21x21", "25x25")),
            new PlantInitializationData("Terminalia arjuna", List.of("25x25")),
            new PlantInitializationData("Terminalia mantaly", List.of("21x21")),
            new PlantInitializationData("Terminalia bellirica", List.of("21x21")),
            new PlantInitializationData("Terminalia catappa", List.of("25x25")),
            new PlantInitializationData("Terminalia bentizoe", List.of("25x25")),
            new PlantInitializationData("Rain Tree", List.of("21x21")),
            new PlantInitializationData("Kadamba Tree", List.of("13x13", "21x21", "25x25")),
            new PlantInitializationData("Mahua Tree", List.of("21x21")),
            new PlantInitializationData("Siris", List.of("21x21")),
            new PlantInitializationData("Indian Elm", List.of()),
            new PlantInitializationData("Putranjiva roxburghii", List.of("21x21")),
            new PlantInitializationData("Kigelia pinnata (Sausage Tree)", List.of("21x21")),
            new PlantInitializationData("Bombax ceiba", List.of("21x21")),
            new PlantInitializationData("Semal", List.of()),
            new PlantInitializationData("Babul", List.of()),
            new PlantInitializationData("Shisham", List.of()),
            new PlantInitializationData("Tabebuia rosea", List.of("21x21", "25x25")),
            new PlantInitializationData("Tabebuia argentea", List.of("25x25")),
            new PlantInitializationData("Spathodea campanulata", List.of("25x25", "50x50")),
            new PlantInitializationData("Lagerstroemia", List.of("30x30")),
            new PlantInitializationData("Lagerstroemia speciosa", List.of("30x30")),
            new PlantInitializationData("Gulmohar Yellow", List.of("40x40")),
            new PlantInitializationData("Parijatak", List.of("30x30")),
            new PlantInitializationData("Plumeria", List.of("18x18", "21x21", "30x30")),
            new PlantInitializationData("Vrundavan Champa", List.of("25x25")),
            new PlantInitializationData("Pong Pong Champa", List.of("21x21")),
            new PlantInitializationData("Hibiscus", List.of("9x11", "25x25")),
            new PlantInitializationData("Hibiscus variegated", List.of("13x13")),
            new PlantInitializationData("Ixora", List.of("9x11", "13x13")),
            new PlantInitializationData("Jatropha", List.of("13x13")),
            new PlantInitializationData("Canna", List.of("13x13")),
            new PlantInitializationData("Allamanda", List.of("13x13")),
            new PlantInitializationData("Turnera ulmifolia", List.of("13x13")),
            new PlantInitializationData("Cestrum nocturnum (Night Queen)", List.of("7x8")),
            new PlantInitializationData("Dracaena", List.of("8x10", "15x16")),
            new PlantInitializationData("Dracaena Mahatma", List.of("15x16")),
            new PlantInitializationData("Dracaena Colorama", List.of("8x10")),
            new PlantInitializationData("Heliconia", List.of("13x13")),
            new PlantInitializationData("Heliconia rostrata", List.of("13x13")),
            new PlantInitializationData("Heliconia psittacorum", List.of("13x13")),
            new PlantInitializationData("Zamia", List.of("15x16", "18x18")),
            new PlantInitializationData("Sansevieria", List.of("pot")),
            new PlantInitializationData("Spathiphyllum", List.of("pot")),
            new PlantInitializationData("Syngonium", List.of("pot")),
            new PlantInitializationData("Coleus", List.of("pot")),
            new PlantInitializationData("Pandanus", List.of("8x10", "13x13")),
            new PlantInitializationData("Rhoeo spathacea", List.of("8x10")),
            new PlantInitializationData("Asparagus", List.of("13x13")),
            new PlantInitializationData("Dianella", List.of("10x11", "13x13")),
            new PlantInitializationData("Wedelia trilobata", List.of("8x8")),
            new PlantInitializationData("Fountain Grass", List.of("8x8", "13x13")),
            new PlantInitializationData("Dragon Fruit", List.of("13x13")),
            new PlantInitializationData("Star Fruit", List.of("21x21")),
            new PlantInitializationData("Beal (Aegle marmelos)", List.of("21x21")),
            new PlantInitializationData("Cannonball Tree", List.of("25x25")),
            new PlantInitializationData("Khaya", List.of("30x30")),
            new PlantInitializationData("Kusum", List.of("25x25")),
            new PlantInitializationData("Badoli Jamun", List.of("30x30")),
            new PlantInitializationData("Chandan Tree", List.of("21x21")),
            new PlantInitializationData("Bauhinia blakeana", List.of("21x21")),
            new PlantInitializationData("Erythrina indica picta", List.of("13x13")),
            new PlantInitializationData("Billipatra", List.of("15x16"))
        );
    }

    private List<PlantInitializationData> loadDefaultPlantsFromResource() {
        try {
            ClassPathResource resource = new ClassPathResource("plants.json");
            if (!resource.exists()) {
                return Collections.emptyList();
            }

            try (InputStream inputStream = resource.getInputStream()) {
                List<PlantMasterEntry> entries = OBJECT_MAPPER.readValue(
                    inputStream,
                    new TypeReference<List<PlantMasterEntry>>() {}
                );

                return entries.stream()
                    .map(entry -> new PlantInitializationData(entry.plant_name, entry.bag_sizes))
                    .toList();
            }
        } catch (Exception ex) {
            log.warn("Failed to load plants.json; falling back to built-in defaults", ex);
            return Collections.emptyList();
        }
    }

    private static class PlantMasterEntry {
        public String plant_name;
        public List<String> bag_sizes;
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private Plant findOwnedPlant(UUID userId, Long plantId) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plant", plantId));
        if (!plant.getUserId().equals(userId)) {
            throw new BusinessException("Access denied: plant does not belong to this user.");
        }
        return plant;
    }

    public String normalize(String name) {
        return name.trim().toLowerCase();
    }

    public String normalizeDimension(String dim) {
        return dim.trim().replaceAll("\\s*[xX]\\s*", "x").toLowerCase();
    }

    /**
     * ✅ CRITICAL FIX: Ensures user exists in the database.
     *
     * This prevents FK constraint violations when:
     * 1. User ID comes from JWT token (Supabase)
     * 2. User hasn't created a profile yet
     * 3. User tries to create a quotation/plant directly
     *
     * Creates a minimal user record if it doesn't exist.
     *
     * @param userId User UUID from JWT token
     */
    private void ensureUserExists(UUID userId) {
        try {
            String checkUserSql = "SELECT 1 FROM users WHERE id = ?::uuid LIMIT 1";
            Integer exists = jdbcTemplate.queryForObject(checkUserSql, Integer.class, userId.toString());

            if (exists != null) {
                return; // User already exists
            }
        } catch (Exception e) {
            // User doesn't exist, continue to create
        }

        try {
            String insertUserSql = "INSERT INTO users (id, auth_method, created_at, updated_at) " +
                                  "VALUES (?::uuid, 'GOOGLE', NOW(), NOW()) " +
                                  "ON CONFLICT (id) DO NOTHING";
            int inserted = jdbcTemplate.update(insertUserSql, userId.toString());

            if (inserted > 0) {
                log.info("✅ Auto-created user record for userId: {}", userId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not ensure user exists for userId: {}. Error: {}", userId, e.getMessage());
            // Don't throw - let the plant insert happen and catch FK violation if needed
        }
    }
}
