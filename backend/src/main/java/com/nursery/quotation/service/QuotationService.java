package com.nursery.quotation.service;

import com.nursery.common.exception.BusinessException;
import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.common.response.PageResponse;
import com.nursery.plant.service.PlantService;
import com.nursery.profile.NurseryProfile;
import com.nursery.profile.NurseryProfileRepository;
import com.nursery.quotation.Quotation;
import com.nursery.quotation.QuotationItem;
import com.nursery.quotation.QuotationRepository;
import com.nursery.quotation.QuotationStatus;
import com.nursery.quotation.dto.QuotationDtos.CreateQuotationRequest;
import com.nursery.quotation.dto.QuotationDtos.QuotationItemRequest;
import com.nursery.quotation.dto.QuotationDtos.QuotationResponse;
import com.nursery.quotation.dto.QuotationDtos.QuotationSummaryResponse;
import com.nursery.quotation.dto.QuotationDtos.UpdateQuotationRequest;
import com.nursery.quotation.mapper.QuotationMapper;
import com.nursery.quotation.numbering.QuotationNumberingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final NurseryProfileRepository profileRepository;
    private final QuotationMapper quotationMapper;
    private final QuotationNumberingService numberingService;
    private final CalculationEngine calculationEngine;
    private final PlantService plantService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public QuotationResponse createQuotation(UUID userId, CreateQuotationRequest request) {
        boolean finalize = Boolean.TRUE.equals(request.shouldFinalize());

        if (finalize) {
            validateFinalQuotationRequest(request.customerName(), request.customerPhone(), request.customerAddress(), request.items());
        }

        Quotation quotation = Quotation.builder()
                .userId(userId)
                .status(finalize ? QuotationStatus.FINAL : QuotationStatus.DRAFT)
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .customerAddress(request.customerAddress())
                .notes(request.notes())
                .heightEnabled(Boolean.TRUE.equals(request.heightEnabled()))
                .loadingChargesEnabled(Boolean.TRUE.equals(request.loadingChargesEnabled()))
                .loadingPricePerTon(request.loadingPricePerTon())
                .year(Year.now().getValue())
                .build();

        validateLoadingCharges(finalize, quotation.isLoadingChargesEnabled(), quotation.getLoadingPricePerTon());
        applyItems(quotation, request.items(), userId);
        calculationEngine.recalculate(quotation);

        if (finalize) {
            assignQuotationNumber(quotation, userId);
            quotation.setFinalizedAt(OffsetDateTime.now());
            quotation.setPublicToken(generatePublicToken());
            quotation.setPublicTokenExpiresAt(OffsetDateTime.now().plusYears(1));
        }

        Quotation saved = quotationRepository.save(quotation);
        log.info("Quotation created [{}] for userId: {}", saved.getStatus(), userId);
        return quotationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public QuotationResponse getQuotation(UUID userId, Long quotationId) {
        Quotation quotation = findOwnedQuotation(userId, quotationId);
        return buildResponseWithNurseryDetails(quotation);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuotationSummaryResponse> listQuotations(UUID userId, String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Quotation> result;

        if (status != null && !status.isBlank()) {
            QuotationStatus parsedStatus = parseStatus(status);
            result = quotationRepository.findByUserIdAndStatusWithSearch(userId, parsedStatus, search, pageable);
        } else {
            result = quotationRepository.findByUserIdWithSearch(userId, search, pageable);
        }

        return PageResponse.from(result.map(quotationMapper::toSummaryResponse));
    }

    @Transactional
    public QuotationResponse updateQuotation(UUID userId, Long quotationId, UpdateQuotationRequest request) {
        Quotation quotation = findOwnedQuotation(userId, quotationId);

        if (quotation.getStatus() == QuotationStatus.FINAL) {
            throw new BusinessException("Finalized quotations cannot be edited.");
        }

        boolean finalize = Boolean.TRUE.equals(request.shouldFinalize());

        if (finalize) {
            validateFinalQuotationRequest(request.customerName(), request.customerPhone(), request.customerAddress(), request.items());
        }

        quotation.setCustomerName(request.customerName());
        quotation.setCustomerPhone(request.customerPhone());
        quotation.setCustomerAddress(request.customerAddress());
        quotation.setNotes(request.notes());
        quotation.setHeightEnabled(Boolean.TRUE.equals(request.heightEnabled()));
        quotation.setLoadingChargesEnabled(Boolean.TRUE.equals(request.loadingChargesEnabled()));
        quotation.setLoadingPricePerTon(request.loadingPricePerTon());

        validateLoadingCharges(finalize, quotation.isLoadingChargesEnabled(), quotation.getLoadingPricePerTon());

        if (request.items() != null) {
            quotation.getItems().clear();
            applyItems(quotation, request.items(), userId);
        }

        calculationEngine.recalculate(quotation);

        if (finalize) {
            quotation.setStatus(QuotationStatus.FINAL);
            assignQuotationNumber(quotation, userId);
            quotation.setFinalizedAt(OffsetDateTime.now());
            quotation.setPublicToken(generatePublicToken());
            quotation.setPublicTokenExpiresAt(OffsetDateTime.now().plusYears(1));
        }

        Quotation saved = quotationRepository.save(quotation);
        log.info("Quotation [{}] updated → status: {}", quotationId, saved.getStatus());
        return quotationMapper.toResponse(saved);
    }

    @Transactional
    public QuotationResponse autoSaveDraft(UUID userId, Long quotationId, UpdateQuotationRequest request) {
        Quotation quotation = findOwnedQuotation(userId, quotationId);

        if (quotation.getStatus() == QuotationStatus.FINAL) {
            throw new BusinessException("Cannot auto-save a finalized quotation.");
        }

        if (request.customerName() != null) quotation.setCustomerName(request.customerName());
        if (request.customerPhone() != null) quotation.setCustomerPhone(request.customerPhone());
        if (request.customerAddress() != null) quotation.setCustomerAddress(request.customerAddress());
        if (request.notes() != null) quotation.setNotes(request.notes());
        if (request.heightEnabled() != null) quotation.setHeightEnabled(request.heightEnabled());
        if (request.loadingChargesEnabled() != null) quotation.setLoadingChargesEnabled(request.loadingChargesEnabled());
        if (request.loadingPricePerTon() != null) quotation.setLoadingPricePerTon(request.loadingPricePerTon());

        validateLoadingCharges(false, quotation.isLoadingChargesEnabled(), quotation.getLoadingPricePerTon());

        if (request.items() != null && !request.items().isEmpty()) {
            quotation.getItems().clear();
            applyItems(quotation, request.items(), userId);
        }

        calculationEngine.recalculate(quotation);

        Quotation saved = quotationRepository.save(quotation);
        log.debug("Draft auto-saved: quotationId={}", quotationId);
        return quotationMapper.toResponse(saved);
    }

    @Transactional
    public void deleteQuotation(UUID userId, Long quotationId) {
        Quotation quotation = findOwnedQuotation(userId, quotationId);
        quotation.setDeletedAt(OffsetDateTime.now());
        quotationRepository.save(quotation);
        log.info("Quotation [{}] soft-deleted for userId: {}", quotationId, userId);
    }

    @Transactional
    public QuotationResponse duplicateQuotation(UUID userId, Long quotationId) {
        Quotation source = findOwnedQuotation(userId, quotationId);

        Quotation copy = Quotation.builder()
                .userId(userId)
                .status(QuotationStatus.DRAFT)
                .customerName(source.getCustomerName())
                .customerPhone(source.getCustomerPhone())
                .customerAddress(source.getCustomerAddress())
                .notes(source.getNotes())
                .heightEnabled(source.isHeightEnabled())
                .loadingChargesEnabled(source.isLoadingChargesEnabled())
                .loadingPricePerTon(source.getLoadingPricePerTon())
                .year(Year.now().getValue())
                .build();

        List<QuotationItem> copiedItems = source.getItems().stream()
                .map(item -> QuotationItem.builder()
                        .quotation(copy)
                        .plantName(item.getPlantName())
                        .dimension(item.getDimension())
                        .height(item.getHeight())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .itemTotal(item.getItemTotal())
                        .build())
                .toList();

        copy.getItems().addAll(copiedItems);
        calculationEngine.recalculate(copy);

        Quotation saved = quotationRepository.save(copy);
        log.info("Quotation [{}] duplicated → new id: {}", quotationId, saved.getId());
        return quotationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public QuotationResponse getByPublicToken(String token) {
        Quotation quotation = quotationRepository.findByPublicToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found or link is invalid."));

        if (quotation.getPublicTokenExpiresAt() != null && quotation.getPublicTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("This quotation link has expired.");
        }

        return buildResponseWithNurseryDetails(quotation);
    }

    private void applyItems(Quotation quotation, List<QuotationItemRequest> itemRequests, UUID userId) {
        if (itemRequests == null) return;

        List<QuotationItem> items = new ArrayList<>();
        for (QuotationItemRequest req : itemRequests) {
            plantService.findOrCreatePlantWithDimension(userId, req.plantName(), req.dimension());

            String height = null;
            if (quotation.isHeightEnabled()) {
                if (req.height() == null || req.height().isBlank()) {
                    throw new BusinessException("Height is required when height is enabled for the quotation.");
                }
                if (!isValidHeightFormat(req.height())) {
                    throw new BusinessException("Height must be like 6 inch, 6 ft, 6 feet, or 6'.");
                }
                height = normalizeHeight(req.height());
            }

            QuotationItem item = QuotationItem.builder()
                    .quotation(quotation)
                    .plantName(req.plantName().trim())
                    .dimension(req.dimension().trim())
                    .height(height)
                    .quantity(req.quantity())
                    .price(req.price())
                    .itemTotal(calculationEngine.calculateItemTotal(req.price(), req.quantity()))
                    .build();
            items.add(item);
        }
        quotation.getItems().addAll(items);
    }

    private void assignQuotationNumber(Quotation quotation, UUID userId) {
        if (quotation.getQuotationNumber() == null) {
            var generated = numberingService.generate(userId);
            quotation.setQuotationNumber(generated.quotationNumber());
            quotation.setYear(generated.year());
            quotation.setSequence(generated.sequence());
        }
    }

    private void validateFinalQuotationRequest(String customerName, String customerPhone, String customerAddress, List<QuotationItemRequest> items) {
        List<String> errors = new ArrayList<>();
        if (customerName == null || customerName.isBlank()) errors.add("Customer name is required for a final quotation.");
        if (customerPhone == null || customerPhone.isBlank()) errors.add("Customer phone is required for a final quotation.");
        if (customerAddress == null || customerAddress.isBlank()) errors.add("Customer address is required for a final quotation.");
        if (items == null || items.isEmpty()) errors.add("At least one item is required for a final quotation.");

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join(" ", errors));
        }
    }

    private void validateLoadingCharges(boolean finalize, boolean loadingChargesEnabled, BigDecimal loadingPricePerTon) {
        if (loadingChargesEnabled && loadingPricePerTon == null) {
            throw new BusinessException(finalize
                    ? "Loading price per ton is required when loading charges are enabled for a final quotation."
                    : "Loading price per ton is required when loading charges are enabled.");
        }
    }

    private boolean isValidHeightFormat(String height) {
        if (height == null) return false;
        String trimmed = height.trim();
        if (trimmed.isEmpty()) return false;

        return trimmed.matches("(?i)^(?:\\d+(?:\\.\\d+)?\\s*(?:in|inch(?:es)?|ft|feet|foot|')|\\d+'\\s*\\d+\\\"?)$");
    }

    private String normalizeHeight(String height) {
        return height == null ? null : height.trim().replaceAll("\\s+", " ");
    }

    private Quotation findOwnedQuotation(UUID userId, Long quotationId) {
        return quotationRepository.findByIdAndUserId(quotationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", quotationId));
    }

    private String generatePublicToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private QuotationStatus parseStatus(String status) {
        try {
            return QuotationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status. Must be DRAFT or FINAL.");
        }
    }

    private QuotationResponse buildResponseWithNurseryDetails(Quotation quotation) {
        NurseryProfile profile = profileRepository.findById(quotation.getUserId()).orElse(null);
        QuotationResponse baseResponse = quotationMapper.toResponse(quotation);

        return new QuotationResponse(
                baseResponse.id(),
                baseResponse.quotationNumber(),
                baseResponse.status(),
                baseResponse.customerName(),
                baseResponse.customerPhone(),
                baseResponse.customerAddress(),
                baseResponse.notes(),
                baseResponse.heightEnabled(),
                profile != null ? profile.getNurseryName() : null,
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getPhone() : null,
                profile != null ? profile.getOwnerName() : null,
                baseResponse.items(),
                baseResponse.loadingChargesEnabled(),
                baseResponse.loadingPricePerTon(),
                baseResponse.loadingChargesAmount(),
                baseResponse.totalAmount(),
                baseResponse.publicToken(),
                baseResponse.finalizedAt(),
                baseResponse.createdAt(),
                baseResponse.updatedAt()
        );
    }
}
