
package com.poorvi.url_shortener.analytics.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poorvi.url_shortener.analytics.event.UrlClickEvent;
import com.poorvi.url_shortener.analytics.service.AnalyticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClickEventConsumer {

    private final ObjectMapper objectMapper;
    private final AnalyticsService analyticsService;

    @KafkaListener(
        topics = "url-clicks",
        groupId = "url-analytics-group"
    )
    public void consume(String message) throws Exception {

        UrlClickEvent event =
                objectMapper.readValue(message, UrlClickEvent.class);

        analyticsService.saveClick(event);

        log.info("Click event processed: {}", event.getEventId());
    }
}
