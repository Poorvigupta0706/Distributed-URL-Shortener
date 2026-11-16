
package com.poorvi.url_shortener.analytics.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlClickEvent {

    private String eventId;
    private String shortCode;
    private LocalDateTime clickedAt;
    private String ipAddress;
    private String userAgent;
    private String referrer;

    public static UrlClickEvent create(
            String shortCode,
            String ipAddress,
            String userAgent,
            String referrer) {

        return UrlClickEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .shortCode(shortCode)
                .clickedAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referrer(referrer)
                .build();
    }
}
