package com.nursery.invoice.service;

import com.nursery.common.exception.BusinessException;
import com.nursery.common.exception.ResourceNotFoundException;
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
                .year(Year.now().getValue())
                .build();

        applyItems(invoice, request.getItems());
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
                        invoice.getTotalAmount(),
                        invoice.getCreatedAt()
                ));
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

        if (request.getItems() != null) {
            invoice.getItems().clear();
            applyItems(invoice, request.getItems());
        }

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
                .year(Year.now().getValue())
                .totalAmount(source.getTotalAmount())
                .build();

        List<InvoiceItem> copied = source.getItems().stream().map(item -> InvoiceItem.builder()
                .invoice(invoice)
                .plantName(item.getPlantName())
                .dimension(item.getDimension())
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
            InvoiceItem it = InvoiceItem.builder()
                    .invoice(invoice)
                    .plantName(r.getPlantName())
                    .dimension(r.getDimension())
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

        return new InvoiceResponse(
                baseResponse.getId(),
                invoiceNumber,
                baseResponse.getStatus(),
                baseResponse.getCustomerName(),
                baseResponse.getCustomerPhone(),
                baseResponse.getCustomerAddress(),
                profile != null ? profile.getNurseryName() : null,
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getPhone() : null,
                profile != null ? profile.getOwnerName() : null,
                baseResponse.getItems(),
                baseResponse.getTotalAmount(),
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
}
