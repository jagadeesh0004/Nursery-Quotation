package com.nursery.pdf;

import com.nursery.config.SecurityUtils;
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

    @GetMapping("/quotations/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        byte[] pdfBytes = pdfGenerationService.generateForQuotation(userId, id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"quotation-" + id + ".pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        byte[] pdfBytes = pdfGenerationService.generateForInvoice(userId, id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-" + id + ".pdf\"")
                .body(pdfBytes);
    }
}
