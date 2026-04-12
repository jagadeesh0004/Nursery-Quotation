package com.nursery.dashboard;

import com.nursery.common.response.ApiResponse;
import com.nursery.config.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard
     * Returns business analytics: total quotations, total revenue, recent activity.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardDtos.DashboardResponse>> getDashboard() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard(userId)));
    }
}
