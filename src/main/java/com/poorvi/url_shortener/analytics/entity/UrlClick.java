
package com.poorvi.url_shortener.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "url_clicks",
    indexes = {
        @Index(name = "idx_click_shortcode_time",
               columnList = "short_code, clicked_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(name = "short_code", nullable = false)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    private String ipAddress;

    @Column(length = 1000)
    private String userAgent;

    private String referrer;
}
