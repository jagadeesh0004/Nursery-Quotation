package com.nursery.dashboard;

import com.nursery.quotation.QuotationRepository;
import com.nursery.quotation.dto.QuotationDtos.QuotationSummaryResponse;
import com.nursery.quotation.mapper.QuotationMapper;
import com.nursery.dashboard.DashboardDtos.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final QuotationRepository quotationRepository;
    private final QuotationMapper quotationMapper;

    private static final int RECENT_COUNT = 5;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID userId) {
        long totalQuotations = quotationRepository.countByUserId(userId);

        BigDecimal totalRevenue = quotationRepository
                .sumTotalAmountByUserIdAndStatusFinal(userId);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        List<QuotationSummaryResponse> recent = quotationRepository
                .findRecentByUserId(userId, RECENT_COUNT)
                .stream()
                .map(quotationMapper::toSummaryResponse)
                .toList();

        return new DashboardResponse(totalQuotations, totalRevenue, recent);
    }
}
