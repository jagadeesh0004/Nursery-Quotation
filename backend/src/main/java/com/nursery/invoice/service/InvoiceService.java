package com.nursery.invoice.service;

import com.nursery.common.exception.BusinessException;
import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.invoice.DiscountType;
import com.nursery.invoice.Invoice;
import com.nursery.invoice.InvoiceItem;
import com.nursery.invoice.InvoiceRepository;
import com.nursery.invoice.dto.InvoiceDtos.*;
import com.nursery.invoice.mapper.InvoiceMapper;
import com.nursery.invoice.numbering.InvoiceNumberingService;
import com.nursery.profile.NurseryProfile;
import com.nursery.profile.NurseryProfileRepository;
import com.nursery.quotation.Quotation;
import com.nursery.quotation.QuotationRepository;
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
    private final AdvancePaymentService advancePaymentService;

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
                .shippingCharges(zeroIfNull(request.getShippingCharges()))
                .year(Year.now().getValue())
                .invoiceDate(OffsetDateTime.now())
                .build();

        applyItems(invoice, request.getItems());
        applyDiscount(invoice, request.getDiscount());
        recalculate(invoice);

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
    public Page<InvoiceSummaryResponse> listInvoices(UUID userId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        NurseryProfile profile = profileRepository.findByUserId(userId).orElse(null);
        return invoiceRepository.findByUserIdWithSearch(userId, search, pageable)
                .map(invoice -> new InvoiceSummaryResponse(
                        invoice.getId(),
                        formatInvoiceNumber(profile, invoice),
                        invoice.getStatus(),
                        invoice.getCustomerName(),
                        resolveSummaryGrandTotal(invoice),
                        invoice.getCreatedAt()
                ));
    }

    @Transactional
    public InvoiceResponse updateInvoice(UUID userId, Long invoiceId, UpdateInvoiceRequest request) {
        Invoice invoice = findOwnedInvoice(userId, invoiceId);

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
        invoice.setShippingCharges(zeroIfNull(request.getShippingCharges()));
        if (invoice.getInvoiceDate() == null) {
            invoice.setInvoiceDate(OffsetDateTime.now());
        }

        if (request.getItems() != null) {
            invoice.getItems().clear();
            applyItems(invoice, request.getItems());
        }

        applyDiscount(invoice, request.getDiscount());
        recalculate(invoice);

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
                .discountType(source.getDiscountType())
                .discountPercentage(source.getDiscountPercentage())
                .discountAmount(zeroIfNull(source.getDiscountAmount()))
                .shippingCharges(zeroIfNull(source.getShippingCharges()))
                .grandTotal(zeroIfNull(source.getGrandTotal()))
                .year(Year.now().getValue())
                .totalAmount(source.getTotalAmount())
                .invoiceDate(OffsetDateTime.now())
                .build();

        List<InvoiceItem> copied = source.getItems().stream().map(item -> InvoiceItem.builder()
                .invoice(invoice)
                .plantName(item.getPlantName())
                .dimension(item.getDimension())
                .height(item.getHeight())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .itemTotal(item.getItemTotal())
                .build()).collect(Collectors.toList());

        invoice.getItems().addAll(copied);
        recalculate(invoice);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice imported from quotation [{}] → new id: {}", quotationId, saved.getId());
        return buildResponseWithNurseryDetails(saved);
    }

    @Transactional
    public void deleteInvoice(UUID userId, Long invoiceId) {
        Invoice invoice = findOwnedInvoice(userId, invoiceId);
        invoice.setDeletedAt(OffsetDateTime.now());
        invoiceRepository.save(invoice);
        log.info("Invoice [{}] soft-deleted for userId: {}", invoiceId, userId);
    }

    private void applyItems(Invoice invoice, List<InvoiceItemRequest> reqs) {
        if (reqs == null) return;
        List<InvoiceItem> items = new ArrayList<>();
        for (InvoiceItemRequest r : reqs) {
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
            InvoiceItem it = InvoiceItem.builder()
                    .invoice(invoice)
                    .plantName(r.getPlantName())
                    .dimension(r.getDimension())
                    .height(height)
                    .quantity(r.getQuantity())
                    .price(r.getPrice())
                    .itemTotal((r.getPrice() == null ? BigDecimal.ZERO : r.getPrice()).multiply(BigDecimal.valueOf(r.getQuantity() == null ? 0 : r.getQuantity())))
                    .build();
            items.add(it);
        }
        invoice.getItems().addAll(items);
    }

    private void recalculate(Invoice invoice) {
        BigDecimal total = invoice.getItems().stream()
                .map(it -> it.getItemTotal() == null ? BigDecimal.ZERO : it.getItemTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setTotalAmount(total);
        BigDecimal shipping = zeroIfNull(invoice.getShippingCharges());
        BigDecimal discount = resolveDiscountAmount(invoice, total);
        invoice.setDiscountAmount(discount);
        invoice.setGrandTotal(total.add(shipping).subtract(discount));
        if (invoice.getGrandTotal().signum() < 0) {
            throw new BusinessException("Grand total cannot be negative.");
        }
    }

    private void applyDiscount(Invoice invoice, DiscountRequest discount) {
        if (discount == null || discount.getType() == null || discount.getValue() == null) {
            invoice.setDiscountType(null);
            invoice.setDiscountPercentage(null);
            invoice.setDiscountAmount(BigDecimal.ZERO);
            return;
        }
        if (discount.getValue().signum() < 0) {
            throw new BusinessException("Discount cannot be negative.");
        }
        invoice.setDiscountType(discount.getType());
        if (discount.getType() == DiscountType.PERCENTAGE) {
            if (discount.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("Discount percentage must be between 0 and 100.");
            }
            invoice.setDiscountPercentage(discount.getValue());
        } else {
            invoice.setDiscountPercentage(null);
            invoice.setDiscountAmount(discount.getValue());
        }
    }

    private BigDecimal resolveDiscountAmount(Invoice invoice, BigDecimal total) {
        if (invoice.getDiscountType() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discountAmount = invoice.getDiscountType() == DiscountType.PERCENTAGE
                ? zeroIfNull(total).multiply(zeroIfNull(invoice.getDiscountPercentage())).divide(BigDecimal.valueOf(100))
                : zeroIfNull(invoice.getDiscountAmount());
        if (discountAmount.compareTo(total) > 0) {
            throw new BusinessException("Discount amount cannot exceed the total amount.");
        }
        return discountAmount;
    }

    private BigDecimal resolveSummaryGrandTotal(Invoice invoice) {
        BigDecimal storedGrandTotal = zeroIfNull(invoice.getGrandTotal());
        if (storedGrandTotal.signum() > 0) {
            return storedGrandTotal;
        }

        BigDecimal itemsTotal = invoice.getItems().stream()
                .map(item -> item.getItemTotal() == null ? BigDecimal.ZERO : item.getItemTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = zeroIfNull(invoice.getShippingCharges());
        BigDecimal discount = resolveDiscountAmount(invoice, itemsTotal);
        BigDecimal computedGrandTotal = itemsTotal.add(shipping).subtract(discount);

        return computedGrandTotal.signum() < 0 ? BigDecimal.ZERO : computedGrandTotal;
    }

    private void assignSequence(Invoice invoice, UUID userId) {
        if (invoice.getSequence() == null) {
            var generated = numberingService.generate(userId);
            invoice.setYear(generated.year());
            invoice.setSequence(generated.sequence());
        }
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
        String invoiceNumber = formatInvoiceNumber(profile, invoice);
        List<AdvancePaymentResponse> advances = advancePaymentService.getAllAdvances(invoice.getUserId(), invoice.getId());
        BigDecimal totalAdvance = advancePaymentService.calculateTotalAdvance(invoice.getId());
        BigDecimal remainingAmount = zeroIfNull(invoice.getGrandTotal()).subtract(totalAdvance);

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
                baseResponse.getTotalAmount(),
                baseResponse.getDiscountType(),
                baseResponse.getDiscountPercentage(),
                baseResponse.getDiscountAmount(),
                baseResponse.getShippingCharges(),
                baseResponse.getGrandTotal(),
                advances,
                totalAdvance,
                remainingAmount,
                invoice.getFinalizedAt(),
                invoice.getCreatedAt()
        );
    }

    private String formatInvoiceNumber(NurseryProfile profile, Invoice invoice) {
        if (invoice.getSequence() == null || invoice.getYear() == null) {
            return "-";
        }

        String initials = profile != null && profile.getInitials() != null && !profile.getInitials().isBlank()
                ? profile.getInitials().toUpperCase()
                : "INV";
        return String.format("INV-%s%04d", initials, invoice.getSequence());
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

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
