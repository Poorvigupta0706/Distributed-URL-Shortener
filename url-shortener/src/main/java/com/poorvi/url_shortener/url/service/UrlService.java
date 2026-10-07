package com.poorvi.url_shortener.url.service;

import com.poorvi.url_shortener.url.dto.UrlResponse;
import com.poorvi.url_shortener.url.entity.ShortUrl;
import com.poorvi.url_shortener.url.repository.ShortUrlRepository;
import com.poorvi.url_shortener.url.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final ShortUrlRepository repository;

    public UrlResponse createShortUrl(String originalUrl) {

        ShortUrl shortUrl = ShortUrl.builder()
                .originalUrl(originalUrl)
                .createdAt(LocalDateTime.now())
                .build();

        shortUrl = repository.save(shortUrl);

        String shortCode = Base62Encoder.encode(shortUrl.getId());

        shortUrl.setShortCode(shortCode);

        repository.save(shortUrl);

        return new UrlResponse(
                "http://localhost:8081/" + shortCode
        );
    }

    public String getOriginalUrl(String shortCode) {

        return repository.findByShortCode(shortCode)
                .map(ShortUrl::getOriginalUrl)
                .orElseThrow(() ->
                        new RuntimeException("Short URL not found"));
    }
}