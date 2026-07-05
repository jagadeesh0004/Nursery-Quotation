package com.nursery.pdf;

import com.nursery.invoice.Invoice;
import com.nursery.invoice.InvoiceItem;
import com.nursery.invoice.InvoiceRepository;
import com.nursery.invoice.InvoiceStatus;
import com.nursery.profile.NurseryProfile;
import com.nursery.profile.NurseryProfileRepository;
import com.nursery.quotation.QuotationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfGenerationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private NurseryProfileRepository profileRepository;

    @InjectMocks
    private PdfGenerationService pdfGenerationService;

    @Test
    void buildInvoiceHtmlV2ShouldMirrorQuotationStructureAndIncludeNotes() throws Exception {
        UUID userId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .id(200L)
                .userId(userId)
                .status(InvoiceStatus.DRAFT)
                .customerName("Test Customer")
                .customerPhone("1234567890")
                .customerAddress("123 Test Street")
                .heightEnabled(true)
                .totalAmount(new BigDecimal("250.00"))
                .notes("Please pay within 7 days")
                .invoiceDate(OffsetDateTime.parse("2024-01-15T10:15:30+00:00"))
                .createdAt(OffsetDateTime.parse("2024-01-15T10:15:30+00:00"))
                .items(List.of(
                        InvoiceItem.builder()
                                .plantName("Rose Plant")
                                .dimension("6\"")
                                .height("18\"")
                                .quantity(10)
                                .price(new BigDecimal("25.00"))
                                .itemTotal(new BigDecimal("250.00"))
                                .build()
                ))
                .build();

        Method method = PdfGenerationService.class.getDeclaredMethod("buildInvoiceHtmlV2", Invoice.class, NurseryProfile.class);
        method.setAccessible(true);

        String html = (String) method.invoke(pdfGenerationService, invoice, null);

        assertTrue(html.contains("INVOICE"));
        assertTrue(html.contains("All Varieties of Fruits, Flowers &amp; Ornamental Plants"));
        assertTrue(html.contains("Notes:"));
        assertTrue(html.contains("Authorized Signature"));
        assertTrue(html.contains("Please pay within 7 days"));
    }

    @Test
    void generateForInvoiceShouldProducePdfBytes() {
        UUID userId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .id(100L)
                .userId(userId)
                .status(InvoiceStatus.DRAFT)
                .customerName("Test Customer")
                .customerPhone("1234567890")
                .customerAddress("123 Test Street")
                .heightEnabled(false)
                .totalAmount(new BigDecimal("250.00"))
                .invoiceDate(OffsetDateTime.parse("2024-01-15T10:15:30+00:00"))
                .createdAt(OffsetDateTime.parse("2024-01-15T10:15:30+00:00"))
                .items(List.of(
                        InvoiceItem.builder()
                                .plantName("Rose Plant")
                                .dimension("6\"")
                                .quantity(10)
                                .price(new BigDecimal("25.00"))
                                .itemTotal(new BigDecimal("250.00"))
                                .build()
                ))
                .build();

        when(invoiceRepository.findByIdAndUserId(100L, userId)).thenReturn(Optional.of(invoice));
        when(profileRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

        byte[] pdfBytes = pdfGenerationService.generateForInvoice(userId, 100L);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertTrue(new String(pdfBytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("%PDF"));
    }
}
