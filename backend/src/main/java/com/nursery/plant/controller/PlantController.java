package com.nursery.plant.controller;

import com.nursery.common.response.ApiResponse;
import com.nursery.config.SecurityUtils;
import com.nursery.plant.dto.PlantDtos.*;
import com.nursery.plant.service.PlantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    /**
     * GET /api/plants
     * Returns all plants with their dimensions for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlantResponse>>> getAllPlants() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(plantService.getAllPlants(userId)));
    }

    /**
     * GET /api/plants/{id}
     * Returns a single plant by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlantResponse>> getPlant(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(plantService.getPlant(userId, id)));
    }

    /**
     * POST /api/plants
     * Creates a new plant in the user's catalog.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PlantResponse>> createPlant(
            @Valid @RequestBody CreatePlantRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PlantResponse response = plantService.createPlant(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Plant created successfully", response));
    }

    /**
     * POST /api/plants/{id}/dimensions
     * Adds a new dimension to an existing plant.
     */
    @PostMapping("/{id}/dimensions")
    public ResponseEntity<ApiResponse<PlantResponse>> addDimension(
            @PathVariable Long id,
            @Valid @RequestBody AddDimensionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PlantResponse response = plantService.addDimension(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Dimension added successfully", response));
    }

    /**
     * DELETE /api/plants/{id}
     * Soft-deletes a plant.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlant(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        plantService.deletePlant(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Plant deleted successfully", null));
    }

    /**
     * DELETE /api/plants/{id}/dimensions/{dimId}
     * Soft-deletes a specific dimension from a plant.
     */
    @DeleteMapping("/{id}/dimensions/{dimId}")
    public ResponseEntity<ApiResponse<Void>> deleteDimension(
            @PathVariable Long id,
            @PathVariable Long dimId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        plantService.deleteDimension(userId, id, dimId);
        return ResponseEntity.ok(ApiResponse.ok("Dimension deleted successfully", null));
    }

    /**
     * POST /api/plants/initialize-defaults
     * Initializes the default plant catalog for the authenticated user.
     */
    @PostMapping("/initialize-defaults")
    public ResponseEntity<ApiResponse<Void>> initializeDefaults() {
        UUID userId = SecurityUtils.getCurrentUserId();
        plantService.initializeDefaultPlantsForUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("Default plants initialized", null));
    }
}
