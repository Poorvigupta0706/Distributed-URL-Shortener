
package com.poorvi.url_shortener.analytics.service;

import com.poorvi.url_shortener.analytics.entity.UrlClick;
import com.poorvi.url_shortener.analytics.event.UrlClickEvent;
import com.poorvi.url_shortener.analytics.repository.UrlClickRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UrlClickRepository repository;

    @Transactional
    public void saveClick(UrlClickEvent event) {

        // Prevent duplicate event processing
        if (repository.existsByEventId(event.getEventId())) {
            return;
        }

        UrlClick click = UrlClick.builder()
                .eventId(event.getEventId())
                .shortCode(event.getShortCode())
                .clickedAt(event.getClickedAt())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .referrer(event.getReferrer())
                .build();

        repository.save(click);
    }

    public long getTotalClicks(String shortCode) {
        return repository.countByShortCode(shortCode);
    }
}
