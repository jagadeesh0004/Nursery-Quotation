package com.nursery.sharing;

import com.nursery.common.response.ApiResponse;
import com.nursery.pdf.PdfGenerationService;
import com.nursery.quotation.dto.QuotationDtos.QuotationResponse;
import com.nursery.quotation.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/quotations")
@RequiredArgsConstructor
public class PublicSharingController {

    private final QuotationService quotationService;
    private final PdfGenerationService pdfGenerationService;

    /**
     * GET /api/public/quotations/{token}
     * Returns quotation details by public token (no auth required).
     */
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<QuotationResponse>> getByToken(
            @PathVariable String token) {
        QuotationResponse response = quotationService.getByPublicToken(token);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * GET /api/public/quotations/{token}/pdf
     * Downloads the PDF for a publicly shared quotation (no auth required).
     */
    @GetMapping("/{token}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String token) {
        byte[] pdf = pdfGenerationService.generateForPublicToken(token);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"quotation.pdf\"")
                .body(pdf);
    }
}
