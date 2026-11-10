package com.aistudymentor.dashboard.controller;

import com.aistudymentor.config.UserPrincipal;
import com.aistudymentor.dashboard.dto.DashboardMetricsResponse;
import com.aistudymentor.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsResponse> getMetrics(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardMetricsResponse response = dashboardService.getMetrics(principal.getUserId());
        return ResponseEntity.ok(response);
    }
}
