
package com.poorvi.url_shortener.analytics.controller;

import com.poorvi.url_shortener.analytics.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{shortCode}")
    public Map<String, Object> getAnalytics(
            @PathVariable String shortCode) {

        return Map.of(
                "shortCode", shortCode,
                "totalClicks",
                analyticsService.getTotalClicks(shortCode)
        );
    }
}
