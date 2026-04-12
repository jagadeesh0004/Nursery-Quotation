package com.nursery.profile.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;
public class ProfileDtos {
    public record ProfileRequest(
             @NotBlank(message = "Nursery name is required")
             @Size(max = 100, message = "Nursery name must not exceed 100 characters")
             String nurseryName,
             @NotBlank(message = "Owner name is required")
             @Size(max = 100, message = "Owner name must not exceed 100 characters")
             String ownerName,
             @NotBlank(message = "Address is required")
             String address,
             @NotBlank(message = "Phone is required")
             @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Invalid phone number format")
             String phone,
             @JsonProperty("logoPath")
             String logoPath,
             String email
     ) {}
     public record ProfileResponse(
             UUID userId,
             String nurseryName,
             String ownerName,
             String address,
             String phone,
             String email,
             String logoPath,
             String initials,
             OffsetDateTime createdAt,
             OffsetDateTime updatedAt
     ) {}
}
