package com.nursery.pdf;

import com.lowagie.text.pdf.BaseFont;
import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.invoice.Invoice;
import com.nursery.invoice.InvoiceItem;
import com.nursery.invoice.InvoiceRepository;
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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates Quotation and Invoice PDFs.
 *
 * <p>
 * Both document types share a single HTML/CSS layout engine
 * ({@link #buildDocumentHtml}
 * and its section builders) so that header, customer block, items table,
 * totals, signature
 * and footer stay pixel-identical between the two documents. Only the color
 * {@link PdfTheme}
 * and the small set of fields called out in the spec (title, document
 * number/date, notes,
 * quotation totals vs. invoice status) differ.
 *
 * <p>
 * Retained behaviour from the previous implementation: rate limiting on all
 * public
 * generation entry points, HTTPS-only + circular-crop logo downloading with
 * caching,
 * placeholder logo fallback, and all quantity/weight/money calculations.
 *
 * <p>
 * New in this revision: proper embedded Unicode font support so Telugu text
 * (customer
 * names, addresses, notes, plant names) renders correctly alongside English
 * text, without
 * being rasterized to an image — copy/paste and text search inside the PDF keep
 * working.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private final QuotationRepository quotationRepository;
    private final InvoiceRepository invoiceRepository;
    private final NurseryProfileRepository profileRepository;
    private final ChromiumPdfService chromiumPdfService;

    private static final String NOTO_TELUGU_BASE64 = FontUtil.loadFontAsBase64("fonts/NotoSansTelugu-Regular.ttf");

    // ------------------------------------------------------------------
    // Formatting constants
    // ------------------------------------------------------------------

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern FIRST_NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Map<String, Double> WEIGHT_MAP = createWeightMap();

    // ------------------------------------------------------------------
    // Layout constants (avoid magic numbers throughout the HTML builders)
    // ------------------------------------------------------------------

    private static final String PAGE_SIDE_MARGIN = "30px";
    private static final int LOGO_SIZE_PX = 94;
    private static final int LOGO_DOWNLOAD_PX = 320;

    // ------------------------------------------------------------------
    // Shared (theme-independent) palette
    // ------------------------------------------------------------------

    private static final String TEXT_PRIMARY = "#333333";
    private static final String TEXT_BODY = "#111111";
    private static final String TEXT_MUTED = "#666666";
    private static final String BORDER_LIGHT = "#e0e0e0";
    private static final String ROW_ALT_BG = "#fafbfc";
    private static final String ROW_BG = "#ffffff";
    private static final String FOOTER_BORDER = "#eeeeee";

    // ------------------------------------------------------------------
    // Telugu Unicode font embedding
    // ------------------------------------------------------------------

    /**
     * Classpath location of the embedded Unicode font. Add a Telugu-capable
     * TrueType/OpenType
     * font here (e.g. Noto Sans Telugu, which also covers Latin script for mixed
     * English +
     * Telugu text) under {@code src/main/resources/fonts/}.
     */
    private static final String UNICODE_FONT_RESOURCE = "/fonts/NotoSansTelugu-Regular.ttf";
    private static final String UNICODE_FONT_FAMILY = "NotoSansTelugu";

    /**
     * Flying Saucer's font resolver needs a real file path, so the classpath font
     * is
     * materialized once to a temp file and reused for the life of the JVM.
     */
    private static volatile File cachedUnicodeFontFile;

    // ==================================================================
    // Public API - unchanged signatures, unchanged security/rate-limit behaviour
    // ==================================================================

    public byte[] generate(Quotation quotation) {
        NurseryProfile profile = profileRepository
                .findByUserId(quotation.getUserId())
                .orElse(null);

        int totalQuantity = quotation.getItems().stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();

        double totalNetWeight = quotation.getItems().stream()
                .mapToDouble(item -> {
                    int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                    return getUnitWeight(item.getDimension()) * quantity;
                })
                .sum();

        DocumentContext context = buildQuotationContext(quotation, profile, totalQuantity, totalNetWeight);
        String html = buildDocumentHtml(context);
        return renderPdf(html, "Quotation PDF generation failed");
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

    @RateLimiter(name = "pdf-generation")
    public byte[] generateForInvoice(UUID userId, Long invoiceId) {
        Invoice invoice = invoiceRepository
                .findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        NurseryProfile profile = profileRepository
                .findByUserId(invoice.getUserId())
                .orElse(null);

        int totalQuantity = invoice.getItems().stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();

        double totalNetWeight = invoice.getItems().stream()
                .mapToDouble(item -> {
                    int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                    return getUnitWeight(item.getDimension()) * quantity;
                })
                .sum();

        DocumentContext context = buildInvoiceContext(invoice, profile, totalQuantity, totalNetWeight);
        String html = buildDocumentHtml(context);
        return renderPdf(html, "Invoice PDF generation failed");
    }

    // ==================================================================
    // PDF rendering (shared by both document types)
    // ==================================================================

    private byte[] renderPdf(String html, String failureMessage) {
        try {
            return chromiumPdfService.generatePdf(html);
        } catch (Exception e) {
            throw new RuntimeException(failureMessage, e);
        }
    }

    /**
     * Registers the embedded Unicode font with Flying Saucer's font resolver so
     * that Telugu
     * (and any other Unicode) glyphs are embedded directly as text - never
     * rasterized - keeping
     * the PDF searchable and its text selectable/copyable.
     */
    private void registerUnicodeFont(ITextRenderer renderer) {
        try {
            File fontFile = resolveUnicodeFontFile();
            renderer.getFontResolver().addFont(
                    fontFile.getAbsolutePath(),
                    UNICODE_FONT_FAMILY,
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    null);
        } catch (Exception ex) {
            // Fall back silently to the renderer's default fonts. English content will
            // still
            // render correctly; only Telugu glyphs would be affected, so we log and
            // continue
            // rather than failing PDF generation.
            log.warn("Unicode font could not be registered ({}); Telugu text may not render "
                    + "correctly until a font is added at classpath:{}", ex.getMessage(), UNICODE_FONT_RESOURCE);
        }
    }

    private synchronized File resolveUnicodeFontFile() throws IOException {
        if (cachedUnicodeFontFile != null && cachedUnicodeFontFile.exists()) {
            return cachedUnicodeFontFile;
        }
        try (InputStream fontStream = PdfGenerationService.class.getResourceAsStream(UNICODE_FONT_RESOURCE)) {
            if (fontStream == null) {
                throw new IOException("Font resource not found on classpath: " + UNICODE_FONT_RESOURCE);
            }
            File tempFont = File.createTempFile("nursery-unicode-font-", ".ttf");
            tempFont.deleteOnExit();
            try (OutputStream os = new FileOutputStream(tempFont)) {
                fontStream.transferTo(os);
            }
            cachedUnicodeFontFile = tempFont;
            return tempFont;
        }
    }

    // ==================================================================
    // Theme + document model
    // ==================================================================

    /**
     * Color palette for one document type. Everything outside this palette (text
     * colors,
     * borders, row striping) is shared between Quotation and Invoice.
     */
    private record PdfTheme(String primary, String primaryDark, String tableHeaderBorder,
            String lightBg, String totalBoxBg, String totalBoxBorder) {
    }

    // Quotation palette - professional slate/blue.
    private static final PdfTheme QUOTATION_THEME = new PdfTheme("#1565c0", "#0d47a1", "#42a5f5", "#f5f7fa", "#eef3fb",
            "#c7d7ef");

    // Invoice palette - nursery green. Intentionally different from the Quotation
    // blues per
    // spec. Adjust these six values if your application already has a different
    // invoice
    // brand color; every invoice visual derives from this one record.
    private static final PdfTheme INVOICE_THEME = new PdfTheme("#2e7d32", "#1b5e20", "#4caf50", "#f5f7f5", "#f1f8f1",
            "#d0e7d2");

    /**
     * One row in the items table. Both {@code QuotationItem} and
     * {@code InvoiceItem} are
     * mapped into this common shape so a single table builder can render either.
     */
    private record LineItem(String plantName, String dimension, String height,
            int quantity, BigDecimal price, BigDecimal itemTotal) {
    }

    /**
     * A single label/value pair shown in the "Quotation Details" / "Invoice
     * Details" box.
     */
    private record InfoRow(String label, String value) {
    }

    /**
     * Everything the shared layout builders need. Populated once per document by
     * {@link #buildQuotationContext} or {@link #buildInvoiceContext}; the section
     * builders
     * only ever read from this object, never branch on "is this a quotation".
     */
    private static final class DocumentContext {
        PdfTheme theme;
        String documentTitle;

        // Nursery / header
        String nurseryName;
        String ownerName;
        String address;
        String phone;
        String email;
        String logoTag;

        // Customer
        String customerName;
        String customerPhone;
        String customerAddress;

        // Document information box (right side, next to customer box)
        String documentInfoHeading;
        List<InfoRow> documentInfoRows;

        // Items
        boolean heightEnabled;
        List<LineItem> lineItems;

        // Totals
        List<String> totalsSummaryLines; // extra lines shown left of the total box (quotation only)
        String totalAmount;

        // Notes - shown for both Quotation and Invoice
        String notes;
    }

    // ==================================================================
    // Context builders - map domain entities onto the shared DocumentContext
    // ==================================================================

    private DocumentContext buildQuotationContext(Quotation quotation, NurseryProfile profile,
            int totalQuantity, double totalNetWeight) {
        DocumentContext ctx = new DocumentContext();
        ctx.theme = QUOTATION_THEME;
        ctx.documentTitle = "QUOTATION";

        populateNurseryDetails(ctx, profile);

        ctx.customerName = e(d(quotation.getCustomerName()));
        ctx.customerPhone = e(d(quotation.getCustomerPhone()));
        ctx.customerAddress = e(d(quotation.getCustomerAddress()));

        ctx.documentInfoHeading = "Quotation Details";
        ctx.documentInfoRows = List.of(
                new InfoRow("Quote ID:", e(d(quotation.getQuotationNumber()))),
                new InfoRow("Date:", formatDate(quotation.getCreatedAt())));

        ctx.heightEnabled = quotation.isHeightEnabled();
        ctx.lineItems = mapQuotationItems(quotation.getItems());

        List<String> quotationSummaryLines = new ArrayList<>();
        quotationSummaryLines.add("Total Quantity: <strong>" + totalQuantity + "</strong>");
        quotationSummaryLines.add("Total Weight: <strong>" + formatWeight(totalNetWeight) + "</strong>");
        if (quotation.isLoadingChargesEnabled()) {
            quotationSummaryLines
                    .add("Loading Charges: <strong>" + formatMoney(quotation.getLoadingChargesAmount()) + "</strong>");
        }
        ctx.totalsSummaryLines = quotationSummaryLines;
        ctx.totalAmount = formatMoney(quotation.getTotalAmount());
        ctx.notes = e(quotation.getNotes());

        return ctx;
    }

    private DocumentContext buildInvoiceContext(Invoice invoice, NurseryProfile profile,
            int totalQuantity, double totalNetWeight) {
        DocumentContext ctx = new DocumentContext();
        ctx.theme = INVOICE_THEME;
        ctx.documentTitle = "INVOICE";

        populateNurseryDetails(ctx, profile);

        ctx.customerName = e(d(invoice.getCustomerName()));
        ctx.customerPhone = e(d(invoice.getCustomerPhone()));
        ctx.customerAddress = e(d(invoice.getCustomerAddress()));

        String invoiceNumber = invoice.getInvoiceNumber() != null && !invoice.getInvoiceNumber().isBlank()
                ? invoice.getInvoiceNumber()
                : "-";
        OffsetDateTime effectiveDate = invoice.getInvoiceDate() != null
                ? invoice.getInvoiceDate()
                : (invoice.getFinalizedAt() != null ? invoice.getFinalizedAt() : invoice.getCreatedAt());

        ctx.documentInfoHeading = "Invoice Details";
        ctx.documentInfoRows = List.of(
                new InfoRow("Invoice:", e(invoiceNumber)),
                new InfoRow("Date:", formatDate(effectiveDate)));

        ctx.heightEnabled = invoice.isHeightEnabled();
        ctx.lineItems = mapInvoiceItems(invoice.getItems());

        List<String> invoiceSummaryLines = new ArrayList<>();
        invoiceSummaryLines.add("Total Quantity: <strong>" + totalQuantity + "</strong>");
        invoiceSummaryLines.add("Total Weight: <strong>" + formatWeight(totalNetWeight) + "</strong>");
        if (invoice.isLoadingChargesEnabled()) {
            invoiceSummaryLines
                    .add("Loading Charges: <strong>" + formatMoney(invoice.getLoadingChargesAmount()) + "</strong>");
        }
        ctx.totalsSummaryLines = invoiceSummaryLines;
        ctx.totalAmount = formatMoney(invoice.getTotalAmount());
        ctx.notes = e(invoice.getNotes());

        return ctx;
    }

    private void populateNurseryDetails(DocumentContext ctx, NurseryProfile profile) {
        ctx.nurseryName = e(profile != null ? profile.getNurseryName() : "Nursery Name");
        ctx.ownerName = e(profile != null ? profile.getOwnerName() : "Owner");
        ctx.address = e(profile != null ? profile.getAddress() : "-");
        ctx.phone = e(profile != null ? profile.getPhone() : "-");
        ctx.email = e(profile != null ? profile.getEmail() : "-");
        ctx.logoTag = buildLogoTag(profile != null ? profile.getLogoPath() : null);
    }

    private List<LineItem> mapQuotationItems(List<QuotationItem> items) {
        List<LineItem> mapped = new ArrayList<>();
        for (QuotationItem item : items) {
            if (item.getPlantName() == null || item.getPlantName().isBlank()) {
                continue;
            }
            mapped.add(new LineItem(
                    e(d(item.getPlantName())),
                    e(d(item.getDimension())),
                    e(d(item.getHeight())),
                    item.getQuantity() == null ? 0 : item.getQuantity(),
                    item.getPrice(),
                    item.getItemTotal()));
        }
        return mapped;
    }

    private List<LineItem> mapInvoiceItems(List<InvoiceItem> items) {
        List<LineItem> mapped = new ArrayList<>();
        for (InvoiceItem item : items) {
            if (item.getPlantName() == null || item.getPlantName().isBlank()) {
                continue;
            }
            mapped.add(new LineItem(
                    e(d(item.getPlantName())),
                    e(d(item.getDimension())),
                    e(d(item.getHeight())),
                    item.getQuantity() == null ? 0 : item.getQuantity(),
                    item.getPrice(),
                    item.getItemTotal()));
        }
        return mapped;
    }

    // ==================================================================
    // HTML document assembly
    // ==================================================================

    private String buildDocumentHtml(DocumentContext ctx) {
        StringBuilder html = new StringBuilder();
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ")
                .append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/>");
        html.append("<style>").append(buildStyleSheet(ctx.theme)).append("</style>");
        html.append("</head>");
        html.append("<body style=\"margin:0;padding:0;\">");

        html.append(buildHeader(ctx));
        html.append(buildDocumentTitle(ctx));
        html.append(buildCustomerAndInfoRow(ctx));
        html.append(buildItemsTable(ctx));
        html.append(buildTotalsSection(ctx));
        html.append(buildNotesAndSignatureRow(ctx));
        html.append(buildFooter(ctx));

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * One stylesheet structure shared by both document types; only the theme colors
     * passed in
     * change per render. Keeping this as a single method - rather than duplicating
     * the CSS in
     * two builder methods - is what keeps the Quotation and Invoice layouts
     * identical.
     */
    private String buildStyleSheet(PdfTheme theme) {
        return "@font-face {"
                + "font-family:'NotoSansTelugu';"
                + "src:url(data:font/truetype;base64,"
                + NOTO_TELUGU_BASE64
                + ") format('truetype');"
                + "font-weight:normal;"
                + "font-style:normal;"
                + "}"

                // (Optional) If you add a real Bold/SemiBold font file under
                // src/main/resources/fonts/, register it here with font-weight:700 so
                // the renderer embeds the authentic bold glyphs. Leaving a single
                // @font-face for the regular file keeps Telugu + Latin rendering stable.
                + "body {"
                + "margin:0;"
                + "padding:0;"
                + "font-family:'NotoSansTelugu','Helvetica Neue',Helvetica,Arial,sans-serif;"
                + "font-size:12px;"
                + "color:" + TEXT_PRIMARY + ";"
                + "background:#fff;"
                + "}"

                + "@page {"
                + "size:A4;"
                + "margin:20px " + PAGE_SIDE_MARGIN + ";"
                + "padding:0;"
                + "}"

                + "* {"
                + "margin:0;"
                + "padding:0;"
                + "}"

                + "table {"
                + "page-break-inside:auto;"
                + "margin:0;"
                + "border-collapse:collapse;"
                + "}"

                + "tr {"
                + "page-break-inside:avoid;"
                + "page-break-after:auto;"
                + "}"

                + "thead { display:table-header-group; }"
                + "tfoot { display:table-footer-group; }"

                + "div {"
                + "margin:0;"
                + "padding:0;"
                + "}"

                + "#footer {"
                + "text-align:center;"
                + "padding:20px 0;"
                + "border-top:1px solid " + FOOTER_BORDER + ";"
                + "color:" + TEXT_MUTED + ";"
                + "font-size:11px;"
                + "margin-top:20px;"
                + "page-break-inside:avoid;"
                + "}"

                // Values in the left-hand totals summary use a <strong> wrapper for the
                // numeric values; make those values darker and bolder for readability.
                + ".totals-summary strong { color: " + TEXT_BODY + "; font-weight:700; }";
    }

    // ------------------------------------------------------------------
    // Shared inline-style fragments
    //
    // These centralize repeated declarations as plain Java strings rather than CSS
    // classes,
    // so the borders/margins/padding stay byte-for-byte identical between Quotation
    // and
    // Invoice without depending on class-selector cascade behaviour.
    // ------------------------------------------------------------------

    private String mutedStyle() {
        return "color:" + TEXT_MUTED + ";";
    }

    private String infoBoxStyle(PdfTheme theme) {
        return "background:" + theme.lightBg() + "; padding:16px; border-radius:6px;";
    }

    private String infoBoxTitleStyle(PdfTheme theme) {
        return "font-size:12px; font-weight:bold; color:" + theme.primary()
                + "; text-transform:uppercase; margin-bottom:10px;";
    }

    private String itemsWrapperStyle() {
        // Only spacing now - the table itself (not this wrapper) owns the border, so
        // there is
        // no second border box that can misalign with the table's own collapsed cell
        // borders.
        return "margin:20px 0 12px 0;";
    }

    private String tableHeaderCellStyle(PdfTheme theme) {
        // Every header cell declares a full 1px border on all four sides. With
        // border-collapse:collapse on the <table>, adjacent cells merge their shared
        // edge into
        // a single line automatically, so no per-column "none" suppression is needed -
        // that is
        // what previously produced gaps/uneven thickness at the header/table boundary.
        return "color:#fff; font-size:11px; font-weight:bold; text-transform:uppercase; padding:12px 8px; "
                + "background:" + theme.primary() + "; border:1px solid " + theme.tableHeaderBorder() + ";";
    }

    private String tableBodyCellStyle() {
        // Same principle as the header cells: a full, uniform border on every side of
        // every
        // cell. border-collapse takes care of merging identical adjacent edges into one
        // clean
        // line, and the outermost cells' borders become the table's visible outer
        // frame.
        return "padding:12px 8px; font-size:12px; font-weight:600; color:" + TEXT_BODY + "; border:1px solid "
                + BORDER_LIGHT + ";";
    }

    private String totalBoxStyle(PdfTheme theme) {
        return "background:" + theme.totalBoxBg() + "; border:1px solid " + theme.totalBoxBorder()
                + "; border-radius:6px; width:100%;";
    }

    // ------------------------------------------------------------------
    // Header
    // ------------------------------------------------------------------

    private String buildHeader(DocumentContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"padding:24px 0 22px 0")
                .append("; border-bottom:1px solid ").append(FOOTER_BORDER).append("; page-break-inside:avoid;\">");
        sb.append("<tr>");
        sb.append("<td width=\"100\" valign=\"top\" style=\"padding-right:16px;text-align:left;\">")
                .append(ctx.logoTag).append("</td>");
        sb.append("<td valign=\"top\" style=\"text-align:left; padding-top:4px;\">");
        sb.append("<div style=\"font-size:28px; font-weight:800; letter-spacing:0.2px; color:")
                .append(ctx.theme.primary())
                .append("; margin-bottom:6px;\">").append(ctx.nurseryName).append("</div>");
        sb.append("<div style=\"font-size:13px; line-height:1.4; margin-bottom:6px; ").append(mutedStyle())
                .append("\">")
                .append("All Varieties of Fruits, Flowers &amp; Ornamental Plants</div>");
        sb.append("<div style=\"font-size:12px;\">").append(ctx.address).append("</div>");
        sb.append("</td>");
        sb.append("<td valign=\"top\" align=\"right\" style=\"padding-top:4px;\">");
        sb.append("<table align=\"right\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        sb.append(headerContactRow("Proprietor:", ctx.ownerName));
        sb.append(headerContactRow("Tel:", ctx.phone));
        sb.append("<tr><td style=\"font-size:12px; padding-right:8px; ").append(mutedStyle()).append("\">Email:</td>")
                .append("<td style=\"font-size:12px;line-height:1.5; font-weight:600;\">").append(ctx.email)
                .append("</td></tr>");
        sb.append("</table>");
        sb.append("</td>");
        sb.append("</tr></table>");
        return sb.toString();
    }

    private String headerContactRow(String label, String value) {
        return "<tr><td style=\"font-size:12px; padding-bottom:6px; padding-right:8px; " + mutedStyle() + "\">" + label
                + "</td><td style=\"font-size:12px; font-weight:bold; padding-bottom:6px;\">" + value + "</td></tr>";
    }

    // ------------------------------------------------------------------
    // Document title (QUOTATION / INVOICE)
    // ------------------------------------------------------------------

    private String buildDocumentTitle(DocumentContext ctx) {
        return "<div style=\"text-align:center; margin-top:20px; margin-bottom:16px; page-break-inside:avoid;\">"
                + "<div style=\"font-size:22px; font-weight:800; color:" + ctx.theme.primary()
                + "; letter-spacing:1px;\">" + ctx.documentTitle + "</div>"
                + "</div>";
    }

    // ------------------------------------------------------------------
    // Customer details + Document information (side by side)
    // ------------------------------------------------------------------

    private String buildCustomerAndInfoRow(DocumentContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"margin-bottom:24px; page-break-inside:avoid;\">");
        sb.append("<tr>");
        sb.append("<td width=\"48%\" valign=\"top\" style=\"").append(infoBoxStyle(ctx.theme)).append("\">")
                .append(buildCustomerSection(ctx)).append("</td>");
        sb.append("<td width=\"4%\"></td>");
        sb.append("<td width=\"48%\" valign=\"top\" style=\"").append(infoBoxStyle(ctx.theme)).append("\">")
                .append(buildDocumentInformation(ctx)).append("</td>");
        sb.append("</tr></table>");
        return sb.toString();
    }

    private String buildCustomerSection(DocumentContext ctx) {
        // The background/padding/border-radius now live on the containing <td> (see
        // buildCustomerAndInfoRow) rather than on a nested <div>. Both cells sit in the
        // same
        // <tr>, so table row layout automatically stretches them to equal height
        // regardless of
        // how many rows either box has - and since the styling is on the cell itself,
        // the
        // colored box visibly fills that full matched height instead of only wrapping
        // its own
        // content height.
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"").append(infoBoxTitleStyle(ctx.theme)).append("\">Customer Details</div>");
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        sb.append(infoBoxRow("Name:", ctx.customerName, "70px"));
        sb.append(infoBoxRow("Phone:", ctx.customerPhone, null));
        sb.append(infoBoxRow("Address:", ctx.customerAddress, null));
        sb.append("</table>");
        return sb.toString();
    }

    private String buildDocumentInformation(DocumentContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"").append(infoBoxTitleStyle(ctx.theme)).append("\">")
                .append(ctx.documentInfoHeading).append("</div>");
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        for (InfoRow row : ctx.documentInfoRows) {
            sb.append(infoBoxRow(row.label(), row.value(), "80px"));
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String infoBoxRow(String label, String value, String labelWidth) {
        String widthAttr = labelWidth != null ? " width=\"" + labelWidth + "\"" : "";
        return "<tr><td" + widthAttr + " style=\"font-size:11px; padding-bottom:6px; " + mutedStyle() + "\">" + label
                + "</td><td style=\"font-size:12px; font-weight:bold; padding-bottom:6px;\">" + value + "</td></tr>";
    }

    // ------------------------------------------------------------------
    // Items table
    // ------------------------------------------------------------------

    private String buildItemsTable(DocumentContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"").append(itemsWrapperStyle()).append("\">");
        // The table's own cells (not an outer border on the <table> element itself)
        // form the
        // visible frame. Flying Saucer's border-collapse implementation does not
        // cleanly merge
        // a table-level border with already-collapsed cell borders - it was adding that
        // width
        // on top of the collapsed cells instead of into them, inflating the table past
        // 100%
        // width and pushing the whole page's content past the A4 boundary. Every cell
        // already
        // declares a full border on all sides, so collapse alone produces a clean,
        // flush frame.
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse; ")
                .append("page-break-inside:auto;\">");
        sb.append(buildItemsTableHeader(ctx.theme, ctx.heightEnabled));
        sb.append("<tbody>");

        int rowNumber = 1;
        for (LineItem item : ctx.lineItems) {
            sb.append(buildItemsTableRow(rowNumber, item, ctx.heightEnabled));
            rowNumber++;
        }

        sb.append("</tbody>");
        sb.append("</table>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildItemsTableHeader(PdfTheme theme, boolean heightEnabled) {
        String nameWidth = heightEnabled ? "24%" : "26%";
        String sizeWidth = heightEnabled ? "12%" : "13%";
        String qtyWidth = heightEnabled ? "14%" : "16%";
        String rateWidth = heightEnabled ? "14%" : "16%";
        String totalWidth = heightEnabled ? "22%" : "17%";
        String headerCell = tableHeaderCellStyle(theme);

        StringBuilder sb = new StringBuilder();
        sb.append("<thead><tr>");
        sb.append("<th style=\"width:6%; text-align:center; ").append(headerCell).append("\">ID</th>");
        sb.append("<th style=\"width:").append(nameWidth).append("; text-align:left; ").append(headerCell)
                .append("\">Plant Name</th>");
        sb.append("<th style=\"width:").append(sizeWidth).append("; text-align:center; ").append(headerCell)
                .append("\">Size</th>");
        if (heightEnabled) {
            sb.append("<th style=\"width:12%; text-align:center; ").append(headerCell).append("\">Height</th>");
        }
        sb.append("<th style=\"width:").append(qtyWidth).append("; text-align:right; ").append(headerCell)
                .append("\">Quantity</th>");
        sb.append("<th style=\"width:").append(rateWidth).append("; text-align:right; ").append(headerCell)
                .append("\">Rate</th>");
        sb.append("<th style=\"width:").append(totalWidth).append("; text-align:right; ").append(headerCell)
                .append("\">Total</th>");
        sb.append("</tr></thead>");
        return sb.toString();
    }

    private String buildItemsTableRow(int rowNumber, LineItem item, boolean heightEnabled) {
        String rowBg = (rowNumber % 2 == 0) ? ROW_ALT_BG : ROW_BG;
        String bodyCell = tableBodyCellStyle();

        StringBuilder sb = new StringBuilder();
        sb.append("<tr style=\"background:").append(rowBg).append(";\">");
        sb.append("<td style=\"text-align:center; ").append(bodyCell).append("\">").append(rowNumber).append("</td>");
        sb.append("<td style=\"text-align:left; ").append(bodyCell).append("\">").append(item.plantName())
                .append("</td>");
        sb.append("<td style=\"text-align:center; ").append(bodyCell).append("\">").append(item.dimension())
                .append("</td>");
        if (heightEnabled) {
            sb.append("<td style=\"text-align:center; ").append(bodyCell).append("\">").append(item.height())
                    .append("</td>");
        }
        sb.append("<td style=\"text-align:right; ").append(bodyCell).append("\">").append(item.quantity())
                .append("</td>");
        sb.append("<td style=\"text-align:right; ").append(bodyCell).append("\">").append(formatMoney(item.price()))
                .append("</td>");
        sb.append("<td style=\"text-align:right; font-weight:500; ").append(bodyCell).append("\">")
                .append(formatMoney(item.itemTotal())).append("</td>");
        sb.append("</tr>");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Totals
    // ------------------------------------------------------------------

    private String buildTotalsSection(DocumentContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"margin-top:20px; margin-bottom:40px; page-break-inside:avoid;\">");
        sb.append("<tr>");
        sb.append("<td width=\"70%\" valign=\"top\" style=\"padding-top:16px;\">");
        for (String line : ctx.totalsSummaryLines) {
            sb.append("<div class=\"totals-summary\" style=\"font-size:12px; margin-bottom:6px; ")
                    .append(mutedStyle()).append("\">")
                    .append(line).append("</div>");
        }
        sb.append("</td>");
        sb.append("<td width=\"30%\" align=\"right\" valign=\"top\">");
        sb.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"").append(totalBoxStyle(ctx.theme))
                .append("\">");
        sb.append("<tr><td style=\"padding:16px 20px; text-align:right;\">");
        sb.append("<div style=\"font-size:13px; font-weight:bold; color:").append(ctx.theme.primary())
                .append("; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:6px;\">Total Amount</div>");
        sb.append("<div style=\"font-size:22px; font-weight:bold; color:").append(ctx.theme.primaryDark())
                .append(";\">").append(ctx.totalAmount).append("</div>");
        sb.append("</td></tr></table>");
        sb.append("</td>");
        sb.append("</tr></table>");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Notes + Signature (rendered as one row so the signature stays pinned to the
    // same position whether or not the notes box is present)
    // ------------------------------------------------------------------

    private String buildNotesAndSignatureRow(DocumentContext ctx) {
        boolean showNotes = ctx.notes != null;

        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"margin-top:20px; page-break-inside:avoid;\">");
        sb.append("<tr>");
        sb.append("<td width=\"35%\" valign=\"bottom\">").append(buildNotesSection(ctx, showNotes)).append("</td>");
        sb.append("<td width=\"30%\"></td>");
        sb.append("<td width=\"35%\" valign=\"bottom\" align=\"right\">").append(buildSignatureSection(ctx))
                .append("</td>");
        sb.append("</tr></table>");
        return sb.toString();
    }

    private String buildNotesSection(DocumentContext ctx, boolean showNotes) {
        if (!showNotes) {
            return ""; // Invoice: column intentionally left blank to keep signature aligned right.
        }
        return "<div style=\"font-size:12px; font-weight:bold; margin-bottom:8px;\">Notes:</div>"
                + "<div style=\"border:1px solid #dddddd; background:" + ROW_ALT_BG
                + "; border-radius:4px; height:90px; padding:8px; white-space:pre-wrap; font-size:12px; color:"
                + TEXT_PRIMARY + ";\">" + ctx.notes + "</div>";
    }

    private String buildSignatureSection(DocumentContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        sb.append("<tr><td style=\"border-bottom:2px solid ").append(TEXT_PRIMARY)
                .append("; height:70px;\"></td></tr>");
        sb.append("<tr><td style=\"text-align:right; font-size:13px; font-weight:bold; padding-top:8px;\">")
                .append("Authorized Signature</td></tr>");
        sb.append("<tr><td style=\"text-align:right; font-size:11px; padding-top:4px; ").append(mutedStyle())
                .append("\">").append(ctx.ownerName).append("</td></tr>");
        sb.append("<tr><td style=\"text-align:right; font-size:11px; padding-top:2px; ").append(mutedStyle())
                .append("\">Proprietor, ").append(ctx.nurseryName).append("</td></tr>");
        sb.append("</table>");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Footer
    // ------------------------------------------------------------------

    private String buildFooter(DocumentContext ctx) {
        return "<div id=\"footer\">"
                + "<div style=\"font-weight:bold; font-size:12px; color:" + ctx.theme.primary()
                + "; margin-bottom:4px;\">Thank you for your business!</div>"
                + "<div>If you have any questions about this document, please contact us at "
                + "<span style=\"color:" + TEXT_PRIMARY + ";\">" + ctx.phone + "</span></div>"
                + "</div>";
    }

    // ==================================================================
    // Logo handling (shared by Quotation and Invoice - previously duplicated)
    // ==================================================================

    private String buildLogoTag(String logoPath) {
        if (logoPath == null || logoPath.isBlank()) {
            log.debug("No logo path provided; using placeholder logo.");
            return placeholderLogoSvg();
        }
        if (logoPath.contains("undefined")) {
            log.warn("Logo URL contains 'undefined' - treating as invalid: {}", logoPath);
            return placeholderLogoSvg();
        }
        try {
            String base64Logo = downloadImageAsBase64(logoPath);
            return "<img src=\"data:image/png;base64," + base64Logo
                    + "\" width=\"" + LOGO_SIZE_PX + "\" height=\"" + LOGO_SIZE_PX + "\" style=\"display:block;\" />";
        } catch (Exception ex) {
            log.warn("Logo download failed ({}); falling back to placeholder.", ex.getMessage());
            return placeholderLogoSvg();
        }
    }

    private String placeholderLogoSvg() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + LOGO_SIZE_PX + "\" height=\"" + LOGO_SIZE_PX
                + "\" style=\"display:inline-block;\">"
                + "<circle cx=\"47\" cy=\"47\" r=\"46\" fill=\"#e8f5e9\" stroke=\"#a5d6a7\" stroke-width=\"2\"/>"
                + "<text x=\"47\" y=\"52\" text-anchor=\"middle\" font-size=\"11\" font-weight=\"bold\" fill=\"#2e7d32\">[LOGO]</text>"
                + "</svg>";
    }

    /**
     * Downloads an image from a URL and returns it as a circular-cropped PNG,
     * base64-encoded.
     * Results are cached for 24 hours (see the "logos" cache config) to avoid
     * repeated downloads.
     * SECURITY: HTTPS-only to reduce SSRF risk; callers must not pass
     * user-controlled schemes.
     */
    @Cacheable(value = "logos", key = "#imageUrl", unless = "#result == null")
    private String downloadImageAsBase64(String imageUrl) throws Exception {
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
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP " + responseCode + " when fetching logo from: " + imageUrl);
        }

        try (InputStream inputStream = connection.getInputStream()) {
            byte[] imageData = inputStream.readAllBytes();
            if (imageData.length == 0) {
                throw new RuntimeException("Downloaded image is empty (0 bytes)");
            }

            byte[] pngBytes = toCircularPng(imageData, LOGO_DOWNLOAD_PX);
            return Base64.getEncoder().encodeToString(pngBytes);
        } finally {
            connection.disconnect();
        }
    }

    private byte[] toCircularPng(byte[] imageData, int size) throws Exception {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageData));
        if (source == null) {
            throw new RuntimeException("ImageIO could not decode logo image");
        }

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int cropSize = Math.min(sourceWidth, sourceHeight);
        int cropX = (sourceWidth - cropSize) / 2;
        int cropY = (sourceHeight - cropSize) / 2;

        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D resizeGraphics = resized.createGraphics();
        resizeGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        resizeGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        resizeGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        resizeGraphics.setClip(new Ellipse2D.Double(0, 0, size, size));
        resizeGraphics.drawImage(source, 0, 0, size, size, cropX, cropY, cropX + cropSize, cropY + cropSize, null);
        resizeGraphics.dispose();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(resized, "png", outputStream);
            return outputStream.toByteArray();
        }
    }

    // ==================================================================
    // Formatting / calculation helpers (unchanged business logic)
    // ==================================================================

    private String formatDate(OffsetDateTime dt) {
        return dt == null ? "-" : DATE_FORMATTER.format(dt);
    }

    private String formatMoney(BigDecimal v) {
        return v == null ? "Rs. 0.00" : "Rs. " + String.format("%.2f", v);
    }

    private String formatWeight(double kg) {
        if (kg >= 1000.0) {
            double tons = kg / 1000.0;
            return String.format(Locale.US, "%.1f tons", tons);
        }
        return String.format(Locale.US, "%.1f kg", kg);
    }

    private static Map<String, Double> createWeightMap() {
        Map<String, Double> map = new HashMap<>();
        // Bag dimensions
        map.put("5x6", 0.5);
        map.put("6x8", 0.9);
        map.put("7x8", 1.3);
        map.put("8x10", 3.3);
        map.put("9x10", 4.5);
        map.put("10x12", 6.0);
        map.put("12x12", 7.5);
        map.put("12x13", 8.0);
        map.put("13x13", 8.0);
        map.put("14x15", 16.0);
        map.put("15x16", 16.0);
        map.put("16x16", 22.0);
        map.put("18x18", 30.0);
        map.put("20x20", 40.0);
        map.put("21x21", 50.0);
        map.put("24x24", 75.0);
        map.put("25x25", 85.0);
        map.put("30x30", 150.0);
        map.put("36x36", 220.0);
        map.put("40x40", 250.0);
        // Pot sizes (inches)
        map.put("3\"", 0.5);
        map.put("4\"", 1.0);
        map.put("5\"", 1.5);
        map.put("6\"", 2.0);
        map.put("7\"", 3.0);
        map.put("8\"", 4.0);
        map.put("9\"", 6.0);
        map.put("10\"", 8.0);
        map.put("11\"", 10.0);
        map.put("12\"", 12.0);
        map.put("13\"", 13.5);
        map.put("14\"", 15.0);
        map.put("15\"", 18.0);
        map.put("16\"", 22.0);
        map.put("17\"", 26.0);
        map.put("18\"", 30.0);
        map.put("20\"", 50.0);
        map.put("24\"", 80.0);
        map.put("30\"", 150.0);
        map.put("36\"", 220.0);
        map.put("40\"", 250.0);
        return map;
    }

    private double getUnitWeight(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return 0.0;
        }

        String k = dimension.trim().toLowerCase(Locale.US).replaceAll("\\s+", "");
        k = k.replace("\u201C", "\"").replace("\u201D", "\"");

        Double v = WEIGHT_MAP.get(k);
        if (v != null) {
            return v;
        }

        // If numeric like '3', try '3"' (matches frontend behaviour).
        if (k.matches("^\\d+(?:\\.\\d+)?$")) {
            String withInch = k + "\"";
            v = WEIGHT_MAP.get(withInch);
            if (v != null) {
                return v;
            }
        }

        // Fallback: extract first number and use it directly (matches frontend
        // behaviour).
        Matcher m = FIRST_NUMBER_PATTERN.matcher(k);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }

        return 0.0;
    }

    /** Displays a placeholder for null/blank/empty-array-looking values. */
    private String d(String v) {
        if (v == null || v.isBlank() || v.equals("[]")) {
            return "-";
        }
        v = v.trim();
        if (v.startsWith("[") && v.endsWith("]")) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v.isEmpty() ? "-" : v;
    }

    /**
     * HTML-escapes a value; returns empty string for null/blank/empty-array-looking
     * values.
     */
    private String e(String v) {
        if (v == null || v.isBlank() || v.equals("[]")) {
            return "";
        }
        v = v.trim();
        if (v.startsWith("[") && v.endsWith("]")) {
            v = v.substring(1, v.length() - 1).trim();
        }
        if (v.isEmpty()) {
            return "";
        }
        return v.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}