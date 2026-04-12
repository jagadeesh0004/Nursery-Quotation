package com.nursery.quotation.service;

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
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (QuotationItem item : quotation.getItems()) {
            BigDecimal itemTotal = calculateItemTotal(item.getPrice(), item.getQuantity());
            item.setItemTotal(itemTotal);
            grandTotal = grandTotal.add(itemTotal);
        }

        quotation.setTotalAmount(grandTotal.setScale(2, RoundingMode.HALF_UP));
    }
}
