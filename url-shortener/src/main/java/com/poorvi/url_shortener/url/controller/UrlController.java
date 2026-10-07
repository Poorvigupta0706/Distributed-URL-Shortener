package com.poorvi.url_shortener.url.controller;

import com.poorvi.url_shortener.url.dto.CreateUrlRequest;
import com.poorvi.url_shortener.url.dto.UrlResponse;
import com.poorvi.url_shortener.url.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/api/urls")
    public ResponseEntity<UrlResponse> createUrl(
            @RequestBody CreateUrlRequest request) {

        return ResponseEntity.ok(
                urlService.createShortUrl(request.getOriginalUrl())
        );
    }
    @GetMapping("/test")
    public String test() {
         return "working";
}

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode) {

        String originalUrl =
                urlService.getOriginalUrl(shortCode);

        return ResponseEntity
                .status(302)
                .location(URI.create(originalUrl))
                .build();
    }
}