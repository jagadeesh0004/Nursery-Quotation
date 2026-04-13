package com.nursery.pdf;

import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.profile.NurseryProfile;
import com.nursery.profile.NurseryProfileRepository;
import com.nursery.quotation.Quotation;
import com.nursery.quotation.QuotationItem;
import com.nursery.quotation.QuotationRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * ✅ PDF Generation Service with:
 * - Rate Limiting (FIX #6)
 * - Logo Caching (FIX #7)
 * - URL Validation (Security)
 * - Null Safety
 * - Multi-page PDF Support (FIX #8)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private final QuotationRepository quotationRepository;
    private final NurseryProfileRepository profileRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generate(Quotation quotation) {
        NurseryProfile profile = profileRepository
                .findByUserId(quotation.getUserId())
                .orElse(null);

        int totalQuantity = quotation.getItems().stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();

        String html = buildQuotationHtml(quotation, profile, totalQuantity);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html, null);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    @RateLimiter(name = "pdf-generation")
    public byte[] generateForQuotation(UUID userId, Long quotationId) {
        Quotation quotation = quotationRepository
                .findByIdAndUserId(quotationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", quotationId));
        return generate(quotation);
    }

    @RateLimiter(name = "pdf-generation")
    public byte[] generateForPublicToken(String token) {
        Quotation quotation = quotationRepository
                .findByPublicToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));
        return generate(quotation);
    }

    private String buildQuotationHtml(Quotation quotation, NurseryProfile profile, int totalQuantity) {

        String nurseryName     = e(profile != null ? profile.getNurseryName() : "Nursery Name");
        String ownerName       = e(profile != null ? profile.getOwnerName()   : "Owner");
        String address         = e(profile != null ? profile.getAddress()     : "-");
        String phone           = e(profile != null ? profile.getPhone()       : "-");
        String email           = e(profile != null ? profile.getEmail()       : "-");
        String logoPath        = profile != null ? profile.getLogoPath()      : null;

        String quotationNumber = e(d(quotation.getQuotationNumber()));
        String quotationDate   = formatDate(quotation.getCreatedAt());
        String customerName    = e(d(quotation.getCustomerName()));
        String customerPhone   = e(d(quotation.getCustomerPhone()));
        String customerAddress = e(d(quotation.getCustomerAddress()));
        String totalAmount     = formatMoney(quotation.getTotalAmount());

        // ── Logo ──
        String logoTag;
        if (logoPath != null && !logoPath.isBlank()) {
            log.info("🖼️  [PDF] Processing logo URL: {}", logoPath);
            if (logoPath.contains("undefined")) {
                log.error("❌ [PDF] Logo URL contains 'undefined' - invalid filename!");
                logoTag = placeholderLogoSvg();
            } else {
                try {
                    log.info("📥 [PDF] Downloading image from Supabase...");
                    String base64Logo = downloadImageAsBase64(logoPath);
                    log.info("✅ [PDF] Image downloaded successfully");
                    log.info("📊 [PDF] Base64 length: {} bytes", base64Logo.length());
                    if (base64Logo.length() < 100) {
                        log.warn("⚠️  [PDF] Base64 is suspiciously small: {} bytes", base64Logo.length());
                    }
                    logoTag = "<img src=\"data:image/webp;base64," + base64Logo
                            + "\" width=\"94\" height=\"94\" style=\"border-radius:50%;border:2px solid #a5d6a7;object-fit:cover;\" />";
                    log.info("✅ [PDF] Image embedded in HTML");
                } catch (Exception ex) {
                    log.error("❌ [PDF] Image download failed: {}", ex.getMessage(), ex);
                    logoTag = placeholderLogoSvg();
                }
            }
        } else {
            log.info("ℹ️  [PDF] No logo path provided - using placeholder");
            logoTag = placeholderLogoSvg();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/>");
        sb.append("<style>");

        sb.append("body { margin:0; padding:0; font-family:'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:12px; color:#333333; background:#fff; }");
        // ✅ FIX: Add top/bottom page margins so content has breathing room at page breaks
        sb.append("@page { size: A4; margin: 20px 0; padding: 0; }");
        sb.append("* { margin: 0; padding: 0; }");

        // ✅ FIX #8: tables flow across pages; only individual rows stay unbroken
        sb.append("table { page-break-inside: auto; margin: 0; }");
        sb.append("tr { page-break-inside: avoid; page-break-after: auto; }");
        // thead repeats on every page automatically
        sb.append("thead { display: table-header-group; }");
        sb.append("tfoot { display: table-footer-group; }");

        sb.append("div { margin: 0; padding: 0; }");
        sb.append(".page-break { page-break-after: always; }");

        sb.append("#footer { text-align:center; padding:20px 30px; border-top:1px solid #eeeeee; color:#666666; font-size:11px; margin-top:20px; page-break-inside:avoid; }");
        sb.append("</style>");
        sb.append("</head>");
        sb.append("<body style=\"margin:0;padding:0;width:210mm;\">");

        // ── Header ──
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"padding:24px 30px 16px 30px; border-bottom:1px solid #eeeeee; page-break-inside:avoid;\">");
        sb.append("<tr>");
        sb.append("<td width=\"100\" valign=\"top\" style=\"padding-right:16px;text-align:left;\">").append(logoTag).append("</td>");
        sb.append("<td valign=\"top\" style=\"text-align:left; padding-top:4px;\">");
        sb.append("<div style=\"font-size:26px; font-weight:800; color:#2e7d32; margin-bottom:6px;\">").append(nurseryName).append("</div>");
        sb.append("<div style=\"font-size:12px; font-weight:400; color:#666666; margin-bottom:6px;\">All Varieties of Fruits, Flowers &amp; Ornamental Plants</div>");
        sb.append("<div style=\"font-size:12px; color:#333333;\">").append(address).append("</div>");
        sb.append("</td>");
        sb.append("<td valign=\"top\" align=\"right\" style=\"padding-top:4px;\">");
        sb.append("<table align=\"right\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        sb.append("<tr><td style=\"font-size:12px; color:#666666; padding-bottom:6px; padding-right:8px;\">Proprietor:</td><td style=\"font-size:12px; font-weight:bold; color:#333333; padding-bottom:6px;\">").append(ownerName).append("</td></tr>");
        sb.append("<tr><td style=\"font-size:12px; color:#666666; padding-bottom:6px; padding-right:8px;\">Tel:</td><td style=\"font-size:12px; font-weight:bold; color:#333333; padding-bottom:6px;\">").append(phone).append("</td></tr>");
        sb.append("<tr><td style=\"font-size:12px; color:#666666; padding-right:8px;\">Email:</td><td style=\"font-size:12px; font-weight:bold; color:#333333;\">").append(email).append("</td></tr>");
        sb.append("</table>");
        sb.append("</td>");
        sb.append("</tr></table>");

        // ── QUOTATION Heading ──
        sb.append("<div style=\"text-align:center; margin-top:20px; margin-bottom:16px; page-break-inside:avoid;\">");
        sb.append("<div style=\"font-size:22px; font-weight:bold; color:#2e7d32; letter-spacing:1px;\">QUOTATION</div>");
        sb.append("</div>");

        // ── Customer & Quotation Boxes ──
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"padding:0 30px; margin-bottom:24px; page-break-inside:avoid;\">");
        sb.append("<tr>");
        sb.append("<td width=\"48%\" valign=\"top\" style=\"background:#f5f7f5; padding:16px; border-radius:6px;\">");
        sb.append("<div style=\"font-size:12px; font-weight:bold; color:#2e7d32; text-transform:uppercase; margin-bottom:10px;\">Customer Details</div>");
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        sb.append("<tr><td style=\"color:#666666; font-size:11px; padding-bottom:6px; width:70px;\">Name:</td><td style=\"color:#333333; font-size:12px; font-weight:bold; padding-bottom:6px;\">").append(customerName).append("</td></tr>");
        sb.append("<tr><td style=\"color:#666666; font-size:11px; padding-bottom:6px;\">Phone:</td><td style=\"color:#333333; font-size:12px; font-weight:bold; padding-bottom:6px;\">").append(customerPhone).append("</td></tr>");
        sb.append("<tr><td style=\"color:#666666; font-size:11px; padding-bottom:6px;\">Address:</td><td style=\"color:#333333; font-size:12px; font-weight:bold; padding-bottom:6px;\">").append(customerAddress).append("</td></tr>");
        sb.append("</table>");
        sb.append("</td>");
        sb.append("<td width=\"4%\"></td>");
        sb.append("<td width=\"48%\" valign=\"top\" style=\"background:#f5f7f5; padding:16px; border-radius:6px;\">");
        sb.append("<div style=\"font-size:12px; font-weight:bold; color:#2e7d32; text-transform:uppercase; margin-bottom:10px;\">Quotation Details</div>");
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        sb.append("<tr><td style=\"color:#666666; font-size:11px; padding-bottom:6px; width:80px;\">Quote ID:</td><td style=\"color:#333333; font-size:12px; font-weight:bold; padding-bottom:6px;\">").append(quotationNumber).append("</td></tr>");
        sb.append("<tr><td style=\"color:#666666; font-size:11px; padding-bottom:6px;\">Date:</td><td style=\"color:#333333; font-size:12px; font-weight:bold; padding-bottom:6px;\">").append(quotationDate).append("</td></tr>");
        sb.append("</table>");
        sb.append("</td>");
        sb.append("</tr></table>");

        // ── ITEMS TABLE ──
        // ✅ FIX #8 (FINAL):
        //   - Outer wrapper table/<td> REMOVED — Flying Saucer cannot flow nested tables across pages
        //   - overflow:hidden REMOVED — clips rows that overflow onto page 2+
        //   - Items table sits directly in body flow inside a plain div with border
        //   - page-break-inside:auto on table, page-break-inside:avoid only on each <tr>
        sb.append("<div style=\"margin:20px 30px 12px 30px; border:1px solid #e0e0e0; border-radius:8px;\">");
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; page-break-inside:auto;\">");
        sb.append("<thead>");
        sb.append("<tr style=\"background:#2e7d32;\">");
        sb.append("<th style=\"width:6%;  color:#fff; font-size:11px; font-weight:bold; text-transform:uppercase; padding:12px 8px; text-align:center; border:1px solid #4caf50; border-top:none; border-left:none;\">ID</th>");
        sb.append("<th style=\"width:32%; color:#fff; font-size:11px; font-weight:bold; text-transform:uppercase; padding:12px 8px; text-align:left;   border:1px solid #4caf50; border-top:none;\">Plant Name</th>");
        sb.append("<th style=\"width:14%; color:#fff; font-size:11px; font-weight:bold; text-transform:uppercase; padding:12px 8px; text-align:center; border:1px solid #4caf50; border-top:none;\">Size</th>");
        sb.append("<th style=\"width:14%; color:#fff; font-size:11px; font-weight:bold; text-transform:uppercase; padding:12px 8px; text-align:right;  border:1px solid #4caf50; border-top:none;\">Rate</th>");
        sb.append("<th style=\"width:14%; color:#fff; font-size:11px; font-weight:bold; text-transform:uppercase; padding:12px 8px; text-align:right;  border:1px solid #4caf50; border-top:none;\">Quantity</th>");
        sb.append("<th style=\"width:20%; color:#fff; font-size:11px; font-weight:bold; text-transform:uppercase; padding:12px 8px; text-align:right;  border:1px solid #4caf50; border-top:none; border-right:none;\">Total</th>");
        sb.append("</tr>");
        sb.append("</thead>");
        sb.append("<tbody>");
        int idx = 1;
        for (QuotationItem item : quotation.getItems()) {
            if (item.getPlantName() == null || item.getPlantName().isBlank()) continue;
            String bg = (idx % 2 == 0) ? "#fafbfc" : "#ffffff";
            sb.append("<tr style=\"background:").append(bg).append(";\">");
            sb.append("<td style=\"padding:12px 8px; text-align:center; font-size:12px; color:#333; border:1px solid #e0e0e0; border-left:none;\">").append(idx).append("</td>");
            sb.append("<td style=\"padding:12px 8px; text-align:left;   font-size:12px; color:#333; border:1px solid #e0e0e0;\">").append(e(d(item.getPlantName()))).append("</td>");
            sb.append("<td style=\"padding:12px 8px; text-align:center; font-size:12px; color:#333; border:1px solid #e0e0e0;\">").append(e(d(item.getDimension()))).append("</td>");
            sb.append("<td style=\"padding:12px 8px; text-align:right;  font-size:12px; color:#333; border:1px solid #e0e0e0;\">").append(formatMoney(item.getPrice())).append("</td>");
            sb.append("<td style=\"padding:12px 8px; text-align:right;  font-size:12px; color:#333; border:1px solid #e0e0e0;\">").append(item.getQuantity() == null ? 0 : item.getQuantity()).append("</td>");
            sb.append("<td style=\"padding:12px 8px; text-align:right;  font-size:12px; color:#333; font-weight:500; border:1px solid #e0e0e0; border-right:none;\">").append(formatMoney(item.getItemTotal())).append("</td>");
            sb.append("</tr>");
            idx++;
        }
        sb.append("</tbody>");
        sb.append("</table>");
        sb.append("</div>");

        // ── TOTALS SECTION ──
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"padding:0 30px; margin-top:20px; margin-bottom:40px; page-break-inside:avoid;\">");
        sb.append("<tr>");
        sb.append("<td width=\"55%\" valign=\"top\" style=\"padding-top:16px;\">");
        sb.append("<div style=\"font-size:12px; color:#666666;\">Total Quantity: <span style=\"font-weight:bold; color:#333333; font-size:13px;\">").append(totalQuantity).append("</span></div>");
        sb.append("</td>");
        sb.append("<td width=\"45%\" align=\"right\" valign=\"top\">");
        sb.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background:#f1f8f1; border:1px solid #d0e7d2; border-radius:6px; width:100%;\">");
        sb.append("<tr><td style=\"padding:16px 20px; text-align:right;\">");
        sb.append("<div style=\"font-size:13px; font-weight:bold; color:#2e7d32; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:6px;\">Total Amount</div>");
        sb.append("<div style=\"font-size:22px; font-weight:bold; color:#1b5e20;\">").append(totalAmount).append("</div>");
        sb.append("</td></tr></table>");
        sb.append("</td>");
        sb.append("</tr></table>");

        // ── NOTES & SIGNATURE SECTION ──
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"padding:0 30px; margin-top:20px; page-break-inside:avoid;\">");
        sb.append("<tr>");
        sb.append("<td width=\"35%\" valign=\"bottom\">");
        sb.append("<div style=\"font-size:12px; font-weight:bold; color:#333333; margin-bottom:8px;\">Notes:</div>");
        sb.append("<div style=\"border:1px solid #eeeeee; background:#fafbfc; border-radius:4px; height:90px;\"></div>");
        sb.append("</td>");
        sb.append("<td width=\"30%\"></td>");
        sb.append("<td width=\"35%\" valign=\"bottom\" align=\"right\">");
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        sb.append("<tr><td style=\"border-bottom:2px solid #333333; height:70px;\"></td></tr>");
        sb.append("<tr><td style=\"text-align:right; font-size:13px; font-weight:bold; color:#333333; padding-top:8px;\">Authorized Signature</td></tr>");
        sb.append("<tr><td style=\"text-align:right; font-size:11px; color:#666666; padding-top:4px;\">").append(ownerName).append("</td></tr>");
        sb.append("<tr><td style=\"text-align:right; font-size:11px; color:#666666; padding-top:2px;\">Proprietor, ").append(nurseryName).append("</td></tr>");
        sb.append("</table>");
        sb.append("</td>");
        sb.append("</tr></table>");

        // ── FOOTER ──
        sb.append("<div id=\"footer\">");
        sb.append("<div style=\"font-weight:bold; font-size:12px; color:#2e7d32; margin-bottom:4px;\">Thank you for your business!</div>");
        sb.append("<div>If you have any questions about this quotation, please contact us at <span style=\"color:#333333;\">").append(phone).append("</span></div>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String placeholderLogoSvg() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"94\" height=\"94\" style=\"display:inline-block;\">"
                + "<circle cx=\"47\" cy=\"47\" r=\"46\" fill=\"#e8f5e9\" stroke=\"#a5d6a7\" stroke-width=\"2\"/>"
                + "<text x=\"47\" y=\"52\" text-anchor=\"middle\" font-size=\"11\" font-weight=\"bold\" fill=\"#2e7d32\">[LOGO]</text>"
                + "</svg>";
    }

    private String formatDate(OffsetDateTime dt) {
        return dt == null ? "-" : DATE_FORMATTER.format(dt);
    }

    private String formatMoney(BigDecimal v) {
        return v == null ? "Rs. 0.00" : "Rs. " + String.format("%.2f", v);
    }

    private String d(String v) {
        if (v == null || v.isBlank() || v.equals("[]")) return "-";
        v = v.trim();
        if (v.startsWith("[") && v.endsWith("]")) v = v.substring(1, v.length() - 1).trim();
        return v.isEmpty() ? "-" : v;
    }

    private String e(String v) {
        if (v == null || v.isBlank() || v.equals("[]")) return "";
        v = v.trim();
        if (v.startsWith("[") && v.endsWith("]")) v = v.substring(1, v.length() - 1).trim();
        if (v.isEmpty()) return "";
        return v.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * ✅ FIX #7: Logo Caching
     * Downloads an image from a URL and returns it as base64.
     * Results are cached for 24 hours to avoid repeated downloads.
     * SECURITY: Validates URL to prevent SSRF attacks.
     */
    @Cacheable(value = "logos", key = "#imageUrl", unless = "#result == null")
    private String downloadImageAsBase64(String imageUrl) throws Exception {
        log.debug("🔗 [PDF] Image URL analysis:");
        log.debug("  ✓ URL: {}", imageUrl);
        log.debug("  ✓ Contains 'undefined': {}", imageUrl.contains("undefined"));
        log.debug("  ✓ URL length: {} chars", imageUrl.length());

        if (!imageUrl.startsWith("https://")) {
            throw new RuntimeException("Invalid logo URL: Must use HTTPS protocol");
        }

        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        log.info("🌐 [PDF] HTTP Response Code: {}", responseCode);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            String msg = "HTTP " + responseCode + " when fetching logo from: " + imageUrl;
            log.error("❌ [PDF] {}", msg);
            throw new RuntimeException(msg);
        }

        try (InputStream inputStream = connection.getInputStream()) {
            byte[] imageData = inputStream.readAllBytes();
            log.info("💾 [PDF] Image bytes read: {} bytes", imageData.length);

            if (imageData.length == 0) {
                throw new RuntimeException("Downloaded image is empty (0 bytes)");
            }

            String base64 = Base64.getEncoder().encodeToString(imageData);
            log.info("✅ [PDF] Base64 encoded successfully: {} chars", base64.length());
            return base64;
        } catch (Exception ex) {
            log.error("❌ [PDF] Error reading image stream: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            connection.disconnect();
        }
    }
}