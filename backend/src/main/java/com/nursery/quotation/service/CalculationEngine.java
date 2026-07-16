package com.nursery.quotation.service;

import com.nursery.common.exception.BusinessException;
import com.nursery.invoice.DiscountType;
import com.nursery.quotation.Quotation;
import com.nursery.quotation.QuotationItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Handles all pricing calculations.
 *
 * Rules:
 *  - item_total = price × quantity  (rounded to 2 decimal places)
 *  - total_amount = sum of all item_totals
 *  - Recalculation happens on every create/update
 */
@Service
public class CalculationEngine {

    /**
     * Calculates itemTotal for a single item and returns it.
     */
    public BigDecimal calculateItemTotal(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
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
}
