package com.nursery.profile.controller;

import com.nursery.common.response.ApiResponse;
import com.nursery.config.SecurityUtils;
import com.nursery.profile.dto.ProfileDtos.ProfileRequest;
import com.nursery.profile.dto.ProfileDtos.ProfileResponse;
import com.nursery.profile.service.NurseryProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nursery.common.exception.ResourceNotFoundException;

import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class NurseryProfileController {

    private final NurseryProfileService profileService;

    /**
     * GET /api/profile
     * Retrieves the nursery profile of the authenticated user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfile(userId)));
    }

    /**
     * PUT /api/profile
     * Creates or fully updates the nursery profile.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> saveProfile(
            @Valid @RequestBody ProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ProfileResponse response = profileService.createOrUpdateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile saved successfully", response));
    }
}
