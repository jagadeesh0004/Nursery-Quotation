package com.nursery.pdf;

import com.nursery.config.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/quotations")
@RequiredArgsConstructor
public class PdfController {

    private final PdfGenerationService pdfGenerationService;

    /**
     * GET /api/quotations/{id}/pdf
     * Downloads the PDF for a quotation (authenticated owner only).
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        byte[] pdfBytes = pdfGenerationService.generateForQuotation(userId, id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"quotation-" + id + ".pdf\"")
                .body(pdfBytes);
    }
}
