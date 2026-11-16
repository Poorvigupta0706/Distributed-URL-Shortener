
package com.poorvi.url_shortener.url.controller;

import com.poorvi.url_shortener.url.dto.CreateUrlRequest;
import com.poorvi.url_shortener.url.dto.UrlResponse;
import com.poorvi.url_shortener.url.service.UrlService;
import com.poorvi.url_shortener.analytics.event.UrlClickEvent;
import com.poorvi.url_shortener.analytics.producer.ClickEventProducer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final ClickEventProducer clickEventProducer;

    // Create a short URL
    @PostMapping("/api/urls")
    public ResponseEntity<UrlResponse> createUrl(
            @Valid @RequestBody CreateUrlRequest request) {

        UrlResponse response = urlService.createShortUrl(
                request.getOriginalUrl(),
                request.getExpiresAt());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // Test endpoint
    @GetMapping("/test")
    public String test() {
        return "working";
    }

    // Delete URL (owner only)
    @DeleteMapping("/api/urls/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable String shortCode) {

        urlService.deleteShortUrl(shortCode);

        return ResponseEntity.noContent().build();
    }

    // Redirect to original URL
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String originalUrl = urlService.getOriginalUrl(shortCode);

        UrlClickEvent event = UrlClickEvent.create(
                shortCode,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        );

        clickEventProducer.publishClickEvent(event);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}
