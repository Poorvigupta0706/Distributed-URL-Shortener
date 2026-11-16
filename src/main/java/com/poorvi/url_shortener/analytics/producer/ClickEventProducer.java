
package com.poorvi.url_shortener.analytics.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poorvi.url_shortener.analytics.event.UrlClickEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClickEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishClickEvent(UrlClickEvent event) {

        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    "url-clicks",
                    event.getShortCode(),
                    message
            ).whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish click event: {}",
                            event.getEventId(), exception);
                } else {
                    log.info("Click event published: {}",
                            event.getEventId());
                }
            });

        } catch (JsonProcessingException exception) {
            log.error("Could not serialize click event", exception);
        }
    }
}
