
package com.poorvi.url_shortener.url.service;

import com.poorvi.url_shortener.url.dto.UrlResponse;
import com.poorvi.url_shortener.url.entity.ShortUrl;
import com.poorvi.url_shortener.url.exception.ExpiredLinkException;
import com.poorvi.url_shortener.url.exception.InvalidUrlException;
import com.poorvi.url_shortener.url.exception.ShortUrlNotFoundException;
import com.poorvi.url_shortener.url.repository.ShortUrlRepository;
import com.poorvi.url_shortener.url.util.Base62Encoder;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final ShortUrlRepository repository;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "url:";
    private static final Duration DEFAULT_CACHE_TTL =
            Duration.ofHours(1);

    @Transactional
    public UrlResponse createShortUrl(
            String originalUrl,
            LocalDateTime expiresAt) {

        validateUrl(originalUrl);

        LocalDateTime now = LocalDateTime.now();

        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new InvalidUrlException(
                    "Expiration time must be in the future");
        }

        ShortUrl shortUrl = ShortUrl.builder()
                .originalUrl(originalUrl.trim())
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        shortUrl = repository.saveAndFlush(shortUrl);

        String shortCode = Base62Encoder.encode(shortUrl.getId());
        shortUrl.setShortCode(shortCode);

        repository.save(shortUrl);

        return new UrlResponse(
                "http://localhost:8081/" + shortCode);
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {

        String cacheKey = CACHE_PREFIX + shortCode;

        // 1. Try Redis first.
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {
            return cachedUrl;
        }

        // 2. Cache miss: look up PostgreSQL.
        ShortUrl shortUrl = repository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(shortCode));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = shortUrl.getExpiresAt();

        // 3. Never cache or redirect an expired URL.
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new ExpiredLinkException(shortCode);
        }

        String originalUrl = shortUrl.getOriginalUrl();

        // 4. Set a TTL that never extends beyond URL expiration.
        Duration ttl = DEFAULT_CACHE_TTL;

        if (expiresAt != null) {
            Duration remaining = Duration.between(now, expiresAt);

            if (remaining.compareTo(ttl) < 0) {
                ttl = remaining;
            }
        }

        // 5. Cache the valid URL.
        if (!ttl.isZero() && !ttl.isNegative()) {
            redisTemplate.opsForValue()
                    .set(cacheKey, originalUrl, ttl);
        }

        return originalUrl;
    }

    private void validateUrl(String originalUrl) {

        if (originalUrl == null || originalUrl.isBlank()) {
            throw new InvalidUrlException(
                    "Original URL is required");
        }

        String value = originalUrl.trim();

        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();

            if (!uri.isAbsolute()
                    || scheme == null
                    || !(scheme.equalsIgnoreCase("http")
                    || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null
                    || uri.getHost().isBlank()
                    || uri.getRawUserInfo() != null) {

                throw new InvalidUrlException(
                        "Enter a valid HTTP or HTTPS URL");
            }

        } catch (IllegalArgumentException ex) {
            throw new InvalidUrlException(
                    "Enter a valid HTTP or HTTPS URL");
        }
    }
}
