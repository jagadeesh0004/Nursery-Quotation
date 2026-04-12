package com.nursery.dashboard;

import com.nursery.quotation.dto.QuotationDtos.QuotationSummaryResponse;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDtos {

    public record DashboardResponse(
            long totalQuotations,
            BigDecimal totalRevenue,
            List<QuotationSummaryResponse> recentQuotations
    ) {}
}
