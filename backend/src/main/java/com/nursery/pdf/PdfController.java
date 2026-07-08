package com.nursery.pdf;

import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.config.SecurityUtils;
import com.nursery.invoice.Invoice;
import com.nursery.invoice.InvoiceRepository;
import com.nursery.quotation.Quotation;
import com.nursery.quotation.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PdfController {

    private final PdfGenerationService pdfGenerationService;
    private final QuotationRepository quotationRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/quotations/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Quotation quotation = quotationRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", id));
        
        byte[] pdfBytes = pdfGenerationService.generate(quotation);
        String filename = "Quotation-" + safeFilenamePart(quotation.getQuotationNumber(), id) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Invoice invoice = invoiceRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        byte[] pdfBytes = pdfGenerationService.generateForInvoice(userId, id);
        String filename = "Invoice-" + safeFilenamePart(invoice.getInvoiceNumber(), id) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    /**
     * Uses the human-readable quotation/invoice number when present, falling back to the
     * numeric id (e.g. a draft invoice that hasn't been finalized/numbered yet). Strips
     * characters that are unsafe in filenames or the Content-Disposition header.
     */
    private String safeFilenamePart(String number, Long id) {
        if (number == null || number.isBlank()) {
            return String.valueOf(id);
        }
        return number.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}