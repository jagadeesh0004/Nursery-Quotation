package com.nursery.quotation.service;

import com.nursery.common.exception.BusinessException;
import com.nursery.invoice.DiscountType;
import com.nursery.invoice.Invoice;
import com.nursery.invoice.InvoiceItem;
import com.nursery.quotation.Quotation;
import com.nursery.quotation.QuotationItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.math.RoundingMode;

/**
 * Handles all pricing calculations.
 *
 * Rules:
 *  - item_total = price × quantity  (rounded to 2 decimal places)
 *  - loading_charges = total_weight_tons × loading_price_per_ton
 *  - total_amount = sum of all item_totals + loading_charges
 *  - Recalculation happens on every create/update
 */
@Service
public class CalculationEngine {

    private static final BigDecimal KG_PER_TON = BigDecimal.valueOf(1000);
    private static final Map<String, Double> WEIGHT_MAP = createWeightMap();

    /**
     * Calculates itemTotal for a single item and returns it.
     */
    public BigDecimal calculateItemTotal(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalWeightKg(Quotation quotation) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (QuotationItem item : quotation.getItems()) {
            totalWeight = totalWeight.add(calculateItemWeightKg(item.getDimension(), item.getQuantity()));
        }
        return totalWeight.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalWeightKg(Invoice invoice) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            totalWeight = totalWeight.add(calculateItemWeightKg(item.getDimension(), item.getQuantity()));
        }
        return totalWeight.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateLoadingCharges(BigDecimal loadingPricePerTon, BigDecimal totalWeightKg) {
        if (loadingPricePerTon == null || totalWeightKg == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalWeightTon = totalWeightKg.divide(KG_PER_TON, 6, RoundingMode.HALF_UP);
        return loadingPricePerTon.multiply(totalWeightTon).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Recalculates and sets itemTotal on every item,
     * then recalculates and sets totalAmount on the quotation.
     */
    public void recalculate(Quotation quotation) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (QuotationItem item : quotation.getItems()) {
            BigDecimal itemTotal = calculateItemTotal(item.getPrice(), item.getQuantity());
            item.setItemTotal(itemTotal);
            totalAmount = totalAmount.add(itemTotal);
        }

        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingCharges = zeroIfNull(quotation.getShippingCharges()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = resolveDiscountAmount(quotation, totalAmount);
        BigDecimal grandTotal = totalAmount.add(shippingCharges).subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        if (grandTotal.signum() < 0) {
            throw new BusinessException("Grand total cannot be negative.");
        }

        quotation.setTotalAmount(totalAmount);
        quotation.setDiscountAmount(discountAmount);
        quotation.setShippingCharges(shippingCharges);
        quotation.setGrandTotal(grandTotal);
    }

    private BigDecimal resolveDiscountAmount(Quotation quotation, BigDecimal totalAmount) {
        if (quotation.getDiscountType() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal discountAmount = quotation.getDiscountType() == DiscountType.PERCENTAGE
                ? totalAmount.multiply(zeroIfNull(quotation.getDiscountPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : zeroIfNull(quotation.getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);

        if (discountAmount.compareTo(totalAmount) > 0) {
            throw new BusinessException("Discount amount cannot exceed the total amount.");
        }

        return discountAmount;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Recalculates and sets itemTotal on every item,
     * then recalculates and sets totalAmount on the invoice.
     */
    public void recalculate(Invoice invoice) {
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (InvoiceItem item : invoice.getItems()) {
            BigDecimal itemTotal = calculateItemTotal(item.getPrice(), item.getQuantity());
            item.setItemTotal(itemTotal);
            grandTotal = grandTotal.add(itemTotal);
        }

        BigDecimal loadingChargesAmount = invoice.isLoadingChargesEnabled()
                ? calculateLoadingCharges(invoice.getLoadingPricePerTon(), calculateTotalWeightKg(invoice))
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        invoice.setLoadingChargesAmount(loadingChargesAmount);
        invoice.setTotalAmount(grandTotal.add(loadingChargesAmount).setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateItemWeightKg(String dimension, int quantity) {
        return BigDecimal.valueOf(getUnitWeight(dimension))
                .multiply(BigDecimal.valueOf(quantity));
    }

    public double getUnitWeight(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return 0.0;
        }

        String normalized = dimension.trim().toLowerCase(Locale.US).replaceAll("\\s+", "");

        Double weight = WEIGHT_MAP.get(normalized);
        if (weight != null) {
            return weight;
        }

        String numeric = normalized.replace("\"", "");
        if (numeric.matches("^\\d+(?:\\.\\d+)?$")) {
            Double inchWeight = WEIGHT_MAP.get(numeric + "\"");
            if (inchWeight != null) {
                return inchWeight;
            }
        }

        return 0.0;
    }

    private static Map<String, Double> createWeightMap() {
        Map<String, Double> map = new HashMap<>();

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
}
