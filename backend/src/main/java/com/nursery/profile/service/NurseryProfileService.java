package com.nursery.profile.service;

import com.nursery.common.exception.BusinessException;
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

    @Transactional
    public ProfileResponse getProfile(UUID userId) {
        NurseryProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Auto-create default profile for new users
                    NurseryProfile newProfile = NurseryProfile.builder()
                        .userId(userId)
                        .nurseryName("New Nursery")
                        .ownerName("Owner")
                        .address("")
                        .phone("")
                        .email(null)
                        .logoPath(null)
                        .initials("NN00")
                        .build();
                    NurseryProfile saved = profileRepository.save(newProfile);
                    log.info("[AUTO] Created default profile for userId: {}", userId);
                    plantService.initializeDefaultPlantsForUserAsync(userId);
                    return saved;
                });
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
             // ⚠️ IMPORTANT: Async initialization needs a small delay to allow the user to be committed to DB
             // This prevents foreign key constraint violations from race conditions
             plantService.initializeDefaultPlantsForUserAsync(userId);
         }

         return profileMapper.toResponse(saved);
     }

    private void applyProfileFields(NurseryProfile profile, ProfileRequest request) {
        profile.setNurseryName(request.nurseryName());
        profile.setOwnerName(request.ownerName());
        profile.setAddress(request.address());
        profile.setPhone(request.phone());
        profile.setEmail(normalizeOptional(request.email()));
        profile.setLogoPath(normalizeOptional(request.logoPath()));
        profile.setInitials(deriveInitials(request.nurseryName()));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Derives initials from the nursery name for quotation numbering.
     * Takes first letter of each word (up to 4), pads with zeros if less than 4 words.
     * e.g. "Go Green Nursery Ltd" -> "GGNL"
     * e.g. "Go Green Nursery" -> "GGN0"
     * e.g. "Go Green" -> "GG00"
     * e.g. "Green" -> "G000"
     */
    public String deriveInitials(String nurseryName) {
        String initials = Arrays.stream(nurseryName.trim().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> String.valueOf(w.charAt(0)).toUpperCase())
                .limit(4)
                .reduce("", String::concat);

        // Pad with zeros to make exactly 4 characters
        return String.format("%-4s", initials).replace(' ', '0');
    }
}
