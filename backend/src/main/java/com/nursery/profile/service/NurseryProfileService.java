package com.nursery.profile.service;

import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.plant.service.PlantService;
import com.nursery.profile.NurseryProfile;
import com.nursery.profile.NurseryProfileRepository;
import com.nursery.profile.dto.ProfileDtos.ProfileRequest;
import com.nursery.profile.dto.ProfileDtos.ProfileResponse;
import com.nursery.profile.mapper.NurseryProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NurseryProfileService {

    private final NurseryProfileRepository profileRepository;
    private final NurseryProfileMapper profileMapper;
    private final FileStorageService fileStorageService;
    private final PlantService plantService;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        NurseryProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nursery profile not found. Please set up your profile."));
        return profileMapper.toResponse(profile);
    }

     @Transactional
     public ProfileResponse createOrUpdateProfile(UUID userId, ProfileRequest request) {
        var existingProfile = profileRepository.findByUserId(userId);
        boolean isNewProfile = existingProfile.isEmpty();
        NurseryProfile profile = existingProfile.orElseGet(() -> NurseryProfile.builder().userId(userId).build());

        applyProfileFields(profile, request);

        NurseryProfile saved;
        try {
            saved = profileRepository.save(profile);
        } catch (DataIntegrityViolationException ex) {
            // Another request likely created the profile concurrently; retry as update.
            NurseryProfile retryProfile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> ex);
            applyProfileFields(retryProfile, request);
            saved = profileRepository.save(retryProfile);
            isNewProfile = false;
        }

        if (isNewProfile) {
            log.info("📌 New user profile created, initializing default plants for userId: {}", userId);
            plantService.initializeDefaultPlantsForUser(userId);
        }

        return profileMapper.toResponse(saved);
     }

    private void applyProfileFields(NurseryProfile profile, ProfileRequest request) {
        profile.setNurseryName(request.nurseryName());
        profile.setOwnerName(request.ownerName());
        profile.setAddress(request.address());
        profile.setPhone(request.phone());
        if (request.email() != null) {
            profile.setEmail(request.email());
        }
        if (request.logoPath() != null) {
            profile.setLogoPath(request.logoPath());
        }
        profile.setInitials(deriveInitials(request.nurseryName()));
    }

    /**
     * Derives initials from the nursery name for quotation numbering.
     * e.g. "Green Valley Nursery" -> "GVN"
     */
    public String deriveInitials(String nurseryName) {
        return Arrays.stream(nurseryName.trim().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> String.valueOf(w.charAt(0)).toUpperCase())
                .limit(3)
                .reduce("", String::concat);
    }
}
