package com.nursery.invoice.controller;

import com.nursery.common.response.ApiResponse;
import com.nursery.config.SecurityUtils;
import com.nursery.invoice.dto.InvoiceDtos.AdvancePaymentRequest;
import com.nursery.invoice.service.AdvancePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdvancePaymentController {

    private final AdvancePaymentService advancePaymentService;

    @PostMapping("/api/invoices/{invoiceId}/advance")
    public ResponseEntity<ApiResponse<?>> addAdvance(@PathVariable Long invoiceId,
                                                     @Valid @RequestBody AdvancePaymentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Advance payment added", advancePaymentService.addAdvance(userId, invoiceId, request)));
    }

    @GetMapping("/api/invoices/{invoiceId}/advance")
    public ResponseEntity<ApiResponse<?>> getAdvances(@PathVariable Long invoiceId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(advancePaymentService.getAllAdvances(userId, invoiceId)));
    }

    @PutMapping("/api/advance/{id}")
    public ResponseEntity<ApiResponse<?>> updateAdvance(@PathVariable Long id,
                                                        @Valid @RequestBody AdvancePaymentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Advance payment updated", advancePaymentService.updateAdvance(userId, id, request)));
    }

    @DeleteMapping("/api/advance/{id}")
    public ResponseEntity<ApiResponse<?>> deleteAdvance(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        advancePaymentService.deleteAdvance(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Advance payment deleted"));
    }
}
