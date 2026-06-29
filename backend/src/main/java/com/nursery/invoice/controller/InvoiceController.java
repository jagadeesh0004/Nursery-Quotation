package com.nursery.invoice.controller;

import com.nursery.common.response.ApiResponse;
import com.nursery.config.SecurityUtils;
import com.nursery.invoice.dto.InvoiceDtos.*;
import com.nursery.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateInvoiceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Invoice created", invoiceService.createInvoice(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(@RequestParam(required = false) String search,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.listInvoices(userId, search, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getInvoice(userId, id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable Long id, @Valid @RequestBody UpdateInvoiceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Invoice updated", invoiceService.updateInvoice(userId, id, request)));
    }

    @PostMapping("/from-quotation/{quotationId}")
    public ResponseEntity<ApiResponse<?>> importFromQuotation(@PathVariable Long quotationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Imported from quotation", invoiceService.importFromQuotation(userId, quotationId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        invoiceService.deleteInvoice(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Invoice deleted"));
    }
}
