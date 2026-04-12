package com.nursery.quotation.controller;

import com.nursery.common.response.ApiResponse;
import com.nursery.common.response.PageResponse;
import com.nursery.config.SecurityUtils;
import com.nursery.quotation.dto.QuotationDtos.*;
import com.nursery.quotation.service.QuotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    /**
     * POST /api/quotations
     * Creates a new quotation. Pass finalize=true to convert directly to FINAL.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QuotationResponse>> create(
            @Valid @RequestBody CreateQuotationRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        QuotationResponse response = quotationService.createQuotation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Quotation created", response));
    }

    /**
     * GET /api/quotations
     * Returns paginated list of quotations. Supports search and status filter.
     *
     * @param search   optional customer name search
     * @param status   optional filter: DRAFT or FINAL
     * @param page     0-based page number
     * @param size     page size (default 10)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QuotationSummaryResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageResponse<QuotationSummaryResponse> result =
                quotationService.listQuotations(userId, search, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * GET /api/quotations/{id}
     * Returns full detail for a single quotation.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationResponse>> getById(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(quotationService.getQuotation(userId, id)));
    }

    /**
     * PUT /api/quotations/{id}
     * Full update of a DRAFT quotation. Pass finalize=true to convert to FINAL.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuotationRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        QuotationResponse response = quotationService.updateQuotation(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Quotation updated", response));
    }

    /**
     * PATCH /api/quotations/{id}/auto-save
     * Auto-saves a DRAFT quotation with partial data. No strict validation.
     */
    @PatchMapping("/{id}/auto-save")
    public ResponseEntity<ApiResponse<QuotationResponse>> autoSave(
            @PathVariable Long id,
            @RequestBody UpdateQuotationRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        QuotationResponse response = quotationService.autoSaveDraft(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Draft saved", response));
    }

    /**
     * DELETE /api/quotations/{id}
     * Soft-deletes a quotation.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        quotationService.deleteQuotation(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Quotation deleted", null));
    }

    /**
     * POST /api/quotations/{id}/duplicate
     * Duplicates an existing quotation as a new DRAFT.
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ApiResponse<QuotationResponse>> duplicate(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        QuotationResponse response = quotationService.duplicateQuotation(userId, id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Quotation duplicated", response));
    }
}
