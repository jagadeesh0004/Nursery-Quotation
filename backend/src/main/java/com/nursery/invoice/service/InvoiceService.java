package com.nursery.invoice.service;

import com.nursery.common.exception.BusinessException;
import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.common.response.PageResponse;
import com.nursery.invoice.Invoice;
import com.nursery.invoice.InvoiceItem;
import com.nursery.invoice.InvoiceRepository;
import com.nursery.invoice.InvoiceStatus;
import com.nursery.invoice.dto.InvoiceDtos.*;
import com.nursery.invoice.mapper.InvoiceMapper;
import com.nursery.invoice.numbering.InvoiceNumberingService;
import com.nursery.plant.service.PlantService;
import com.nursery.profile.NurseryProfile;
import com.nursery.profile.NurseryProfileRepository;
import com.nursery.quotation.Quotation;
import com.nursery.quotation.QuotationRepository;
import com.nursery.quotation.service.CalculationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final QuotationRepository quotationRepository;
    private final NurseryProfileRepository profileRepository;
    private final InvoiceNumberingService numberingService;
    private final CalculationEngine calculationEngine;
    private final PlantService plantService;

    // ─── Create ──────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse createInvoice(UUID userId, CreateInvoiceRequest request) {
        boolean finalize = Boolean.TRUE.equals(request.getShouldFinalize());

        if (finalize) {
            validateFinalInvoiceRequest(
                    request.getCustomerName(),
                    request.getCustomerPhone(),
                    request.getCustomerAddress(),
                    request.getItems());
        }

        Invoice invoice = Invoice.builder()
                .userId(userId)
                .status(finalize ? com.nursery.invoice.InvoiceStatus.FINAL : com.nursery.invoice.InvoiceStatus.DRAFT)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerAddress(request.getCustomerAddress())
                .notes(request.getNotes())
                .heightEnabled(Boolean.TRUE.equals(request.getHeightEnabled()))
            .loadingChargesEnabled(Boolean.TRUE.equals(request.getLoadingChargesEnabled()))
            .loadingPricePerTon(request.getLoadingPricePerTon())
                .year(Year.now().getValue())
                .invoiceDate(OffsetDateTime.now())
                .build();

        validateLoadingCharges(finalize, invoice.isLoadingChargesEnabled(), invoice.getLoadingPricePerTon());
        applyItems(invoice, request.getItems(), userId);
        calculationEngine.recalculate(invoice);

        if (finalize) {
            assignSequence(invoice, userId);
            invoice.setFinalizedAt(OffsetDateTime.now());
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created [{}] for userId: {}", saved.getStatus(), userId);
        return buildResponseWithNurseryDetails(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID userId, Long invoiceId) {
        Invoice invoice = findOwnedInvoice(userId, invoiceId);
        return buildResponseWithNurseryDetails(invoice);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummaryResponse> listInvoices(UUID userId, String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        NurseryProfile profile = profileRepository.findByUserId(userId).orElse(null);
        Page<Invoice> result;

        if (status != null && !status.isBlank()) {
            InvoiceStatus parsedStatus = parseStatus(status);
            result = invoiceRepository.findByUserIdAndStatusWithSearch(userId, parsedStatus, search, pageable);
        } else {
            result = invoiceRepository.findByUserIdWithSearch(userId, search, pageable);
        }

        Page<InvoiceSummaryResponse> mapped = result.map(invoice -> new InvoiceSummaryResponse(
                invoice.getId(),
                getInvoiceNumber(invoice, profile),
                invoice.getStatus(),
                invoice.getCustomerName(),
                invoice.getTotalAmount(),
                invoice.getCreatedAt()
        ));

        return PageResponse.from(mapped);
    }

    @Transactional
    public InvoiceResponse updateInvoice(UUID userId, Long invoiceId, UpdateInvoiceRequest request) {
        Invoice invoice = findOwnedInvoice(userId, invoiceId);

        if (invoice.getStatus() == com.nursery.invoice.InvoiceStatus.FINAL) {
            throw new BusinessException("Finalized invoices cannot be edited.");
        }

        boolean finalize = Boolean.TRUE.equals(request.getShouldFinalize());

        if (finalize) {
            validateFinalInvoiceRequest(
                    request.getCustomerName(),
                    request.getCustomerPhone(),
                    request.getCustomerAddress(),
                    request.getItems());
        }

        invoice.setCustomerName(request.getCustomerName());
        invoice.setCustomerPhone(request.getCustomerPhone());
        invoice.setCustomerAddress(request.getCustomerAddress());
        invoice.setNotes(request.getNotes());
        invoice.setHeightEnabled(Boolean.TRUE.equals(request.getHeightEnabled()));
        invoice.setLoadingChargesEnabled(Boolean.TRUE.equals(request.getLoadingChargesEnabled()));
        invoice.setLoadingPricePerTon(request.getLoadingPricePerTon());
        if (invoice.getInvoiceDate() == null) {
            invoice.setInvoiceDate(OffsetDateTime.now());
        }

        validateLoadingCharges(finalize, invoice.isLoadingChargesEnabled(), invoice.getLoadingPricePerTon());

        if (request.getItems() != null) {
            invoice.getItems().clear();
            applyItems(invoice, request.getItems(), userId);
        }

        calculationEngine.recalculate(invoice);

        if (finalize) {
            invoice.setStatus(com.nursery.invoice.InvoiceStatus.FINAL);
            assignSequence(invoice, userId);
            invoice.setFinalizedAt(OffsetDateTime.now());
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice [{}] updated → status: {}", invoiceId, saved.getStatus());
        return buildResponseWithNurseryDetails(saved);
    }

    @Transactional
    public InvoiceResponse importFromQuotation(UUID userId, Long quotationId) {
        Quotation source = quotationRepository.findByIdAndUserId(quotationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        Invoice invoice = Invoice.builder()
                .userId(userId)
                .status(com.nursery.invoice.InvoiceStatus.DRAFT)
                .customerName(source.getCustomerName())
                .customerPhone(source.getCustomerPhone())
                .customerAddress(source.getCustomerAddress())
                .notes(source.getNotes())
                .heightEnabled(source.isHeightEnabled())
            .loadingChargesEnabled(source.isLoadingChargesEnabled())
            .loadingPricePerTon(source.getLoadingPricePerTon())
                .year(Year.now().getValue())
                .invoiceDate(OffsetDateTime.now())
                .build();

        List<InvoiceItem> copied = source.getItems().stream().map(item -> {
            plantService.findOrCreatePlantWithDimension(userId, item.getPlantName(), item.getDimension());
            return InvoiceItem.builder()
                    .invoice(invoice)
                    .plantName(item.getPlantName())
                    .dimension(item.getDimension())
                    .height(item.getHeight())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .itemTotal(item.getItemTotal())
                    .build();
        }).collect(Collectors.toList());

        invoice.getItems().addAll(copied);
        calculationEngine.recalculate(invoice);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice imported from quotation [{}] → new id: {}", quotationId, saved.getId());
        return buildResponseWithNurseryDetails(saved);
    }

    @Transactional
    public InvoiceResponse autoSaveDraft(UUID userId, Long invoiceId, UpdateInvoiceRequest request) {
        Invoice invoice = findOwnedInvoice(userId, invoiceId);

        if (invoice.getStatus() == InvoiceStatus.FINAL) {
            throw new BusinessException("Cannot auto-save a finalized invoice.");
        }

        if (request.getCustomerName() != null) invoice.setCustomerName(request.getCustomerName());
        if (request.getCustomerPhone() != null) invoice.setCustomerPhone(request.getCustomerPhone());
        if (request.getCustomerAddress() != null) invoice.setCustomerAddress(request.getCustomerAddress());
        if (request.getNotes() != null) invoice.setNotes(request.getNotes());
        if (request.getHeightEnabled() != null) invoice.setHeightEnabled(request.getHeightEnabled());
        if (request.getLoadingChargesEnabled() != null) invoice.setLoadingChargesEnabled(request.getLoadingChargesEnabled());
        if (request.getLoadingPricePerTon() != null) invoice.setLoadingPricePerTon(request.getLoadingPricePerTon());

        if (request.getItems() != null) {
            invoice.getItems().clear();
            applyItems(invoice, request.getItems(), userId);
        }

        calculationEngine.recalculate(invoice);

        Invoice saved = invoiceRepository.save(invoice);
        log.debug("Draft auto-saved: invoiceId={}", invoiceId);
        return buildResponseWithNurseryDetails(saved);
    }

    @Transactional
    public void deleteInvoice(UUID userId, Long invoiceId) {
        Invoice invoice = findOwnedInvoice(userId, invoiceId);
        invoice.setDeletedAt(OffsetDateTime.now());
        invoiceRepository.save(invoice);
        log.info("Invoice [{}] soft-deleted for userId: {}", invoiceId, userId);
    }

    private void applyItems(Invoice invoice, List<InvoiceItemRequest> reqs, UUID userId) {
        if (reqs == null) return;
        List<InvoiceItem> items = new ArrayList<>();
        for (InvoiceItemRequest r : reqs) {
            if (r.getPlantName() == null || r.getPlantName().isBlank()) {
                throw new BusinessException("Plant name is required for each invoice item.");
            }
            if (r.getDimension() == null || r.getDimension().isBlank()) {
                throw new BusinessException("Dimension is required for each invoice item.");
            }

            plantService.findOrCreatePlantWithDimension(userId, r.getPlantName(), r.getDimension());

            String height = null;
            if (invoice.isHeightEnabled()) {
                if (r.getHeight() == null || r.getHeight().isBlank()) {
                    throw new BusinessException("Height is required when height is enabled for the invoice.");
                }
                if (!isValidHeightFormat(r.getHeight())) {
                    throw new BusinessException("Height must be like 6 inch, 6 ft, 6 feet, or 6'.");
                }
                height = normalizeHeight(r.getHeight());
            }

            BigDecimal price = r.getPrice() == null ? BigDecimal.ZERO : r.getPrice();
            int quantity = r.getQuantity() == null ? 0 : r.getQuantity();
            InvoiceItem it = InvoiceItem.builder()
                    .invoice(invoice)
                    .plantName(r.getPlantName().trim())
                    .dimension(r.getDimension().trim())
                    .height(height)
                    .quantity(quantity)
                    .price(price)
                    .itemTotal(calculationEngine.calculateItemTotal(price, quantity))
                    .build();
            items.add(it);
        }
        invoice.getItems().addAll(items);
    }

    private void recalculate(Invoice invoice) {
        calculationEngine.recalculate(invoice);
    }

    private void assignSequence(Invoice invoice, UUID userId) {
        if (invoice.getInvoiceNumber() != null && !invoice.getInvoiceNumber().isBlank()) {
            return;
        }

        if (invoice.getSequence() != null) {
            NurseryProfile profile = profileRepository.findByUserId(userId).orElse(null);
            String initials = profile != null && profile.getInitials() != null && !profile.getInitials().isBlank()
                    ? profile.getInitials().toUpperCase()
                    : "0000";
            invoice.setInvoiceNumber(String.format("INV-%s%04d", initials, invoice.getSequence()));
            return;
        }

        var generated = numberingService.generate(userId);
        invoice.setInvoiceNumber(generated.invoiceNumber());
        invoice.setYear(generated.year());
        invoice.setSequence(generated.sequence());
    }

    private void validateFinalInvoiceRequest(
            String customerName, String customerPhone,
            String customerAddress, List<InvoiceItemRequest> items) {

        List<String> errors = new ArrayList<>();
        if (customerName == null || customerName.isBlank())
            errors.add("Customer name is required for a final invoice.");
        if (customerPhone == null || customerPhone.isBlank())
            errors.add("Customer phone is required for a final invoice.");
        if (customerAddress == null || customerAddress.isBlank())
            errors.add("Customer address is required for a final invoice.");
        if (items == null || items.isEmpty())
            errors.add("At least one item is required for a final invoice.");

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join(" ", errors));
        }
    }

    private Invoice findOwnedInvoice(UUID userId, Long invoiceId) {
        return invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
    }

    private InvoiceResponse buildResponseWithNurseryDetails(Invoice invoice) {
        NurseryProfile profile = profileRepository.findByUserId(invoice.getUserId()).orElse(null);
        InvoiceResponse baseResponse = invoiceMapper.toResponse(invoice);
        String invoiceNumber = getInvoiceNumber(invoice, profile);

        return new InvoiceResponse(
                baseResponse.getId(),
                invoiceNumber,
                baseResponse.getStatus(),
                baseResponse.getCustomerName(),
                baseResponse.getCustomerPhone(),
                baseResponse.getCustomerAddress(),
                baseResponse.getNotes(),
                baseResponse.getHeightEnabled(),
                profile != null ? profile.getNurseryName() : null,
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getPhone() : null,
                profile != null ? profile.getOwnerName() : null,
                baseResponse.getItems(),
                baseResponse.getLoadingChargesEnabled(),
                baseResponse.getLoadingPricePerTon(),
                baseResponse.getLoadingChargesAmount(),
                baseResponse.getTotalAmount(),
                invoice.getFinalizedAt(),
                invoice.getCreatedAt()
        );
    }

    private void validateLoadingCharges(boolean finalize, boolean loadingChargesEnabled, BigDecimal loadingPricePerTon) {
        if (loadingChargesEnabled && loadingPricePerTon == null) {
            throw new BusinessException(finalize
                    ? "Loading price per ton is required when loading charges are enabled for a final invoice."
                    : "Loading price per ton is required when loading charges are enabled.");
        }
    }

    private String getInvoiceNumber(Invoice invoice, NurseryProfile profile) {
        if (invoice.getStatus() == com.nursery.invoice.InvoiceStatus.DRAFT) {
            return "-";
        }

        if (invoice.getInvoiceNumber() != null && !invoice.getInvoiceNumber().isBlank()) {
            return invoice.getInvoiceNumber();
        }

        if (invoice.getSequence() == null || invoice.getYear() == null) {
            return "-";
        }

        String initials = profile != null && profile.getInitials() != null && !profile.getInitials().isBlank()
                ? profile.getInitials().toUpperCase()
                : "INV";
        return String.format("INV-%s%04d", initials, invoice.getSequence());
    }

    private InvoiceStatus parseStatus(String status) {
        try {
            return InvoiceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status. Must be DRAFT or FINAL.");
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
}
