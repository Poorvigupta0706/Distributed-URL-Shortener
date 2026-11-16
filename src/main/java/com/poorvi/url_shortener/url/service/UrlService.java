
package com.poorvi.url_shortener.url.service;

import com.poorvi.url_shortener.url.dto.UrlResponse;
import com.poorvi.url_shortener.url.entity.ShortUrl;
import com.poorvi.url_shortener.url.exception.ExpiredLinkException;
import com.poorvi.url_shortener.url.exception.InvalidUrlException;
import com.poorvi.url_shortener.url.exception.ShortUrlNotFoundException;
import com.poorvi.url_shortener.url.repository.ShortUrlRepository;
import com.poorvi.url_shortener.url.util.Base62Encoder;
import com.poorvi.url_shortener.user.entity.user;
import com.poorvi.url_shortener.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final ShortUrlRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    private static final String CACHE_PREFIX = "url:";

    private static final Duration DEFAULT_CACHE_TTL =
            Duration.ofHours(1);

    // Create a short URL for the authenticated user
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

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new IllegalStateException(
                    "User is not authenticated");
        }

        String email = authentication.getName();

        user owner = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"));

        ShortUrl shortUrl = ShortUrl.builder()
                .originalUrl(originalUrl.trim())
                .createdAt(now)
                .expiresAt(expiresAt)
                .owner(owner)
                .build();

        shortUrl = repository.saveAndFlush(shortUrl);

        String shortCode = Base62Encoder.encode(shortUrl.getId());

        shortUrl.setShortCode(shortCode);

        repository.save(shortUrl);

        return new UrlResponse(
                "http://localhost:8081/" + shortCode);
    }

    // Delete a short URL and invalidate its Redis cache
    @Transactional
    public void deleteShortUrl(String shortCode) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new IllegalStateException(
                    "User is not authenticated");
        }

        String email = authentication.getName();

        ShortUrl shortUrl = repository
                .findByShortCodeAndOwner_Email(shortCode, email)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(shortCode));

        repository.delete(shortUrl);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        redisTemplate.delete(
                                CACHE_PREFIX + shortCode);
                    }
                }
        );
    }

    // Get original URL using Redis cache or PostgreSQL
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {

        String cacheKey = CACHE_PREFIX + shortCode;

        // 1. Check Redis cache
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {

            Long remainingTtl = redisTemplate.getExpire(
                    cacheKey,
                    TimeUnit.MILLISECONDS
            );

            // 2. Valid cache entry
            if (remainingTtl != null && remainingTtl > 0) {

                log.info(
                        "Redis cache HIT for shortCode: {}",
                        shortCode
                );

                return cachedUrl;
            }

            // 3. Entry without a valid TTL
            log.warn(
                    "Redis cache entry has invalid TTL for shortCode: {}",
                    shortCode
            );

            redisTemplate.delete(cacheKey);
        }

        // 4. Redis cache MISS
        log.info(
                "Redis cache MISS for shortCode: {}",
                shortCode
        );

        ShortUrl shortUrl = repository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(shortCode));

        // 5. Validate URL expiration
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime expiresAt = shortUrl.getExpiresAt();

        if (expiresAt != null && !expiresAt.isAfter(now)) {

            redisTemplate.delete(cacheKey);

            throw new ExpiredLinkException(shortCode);
        }

        // 6. Get original URL
        String originalUrl = shortUrl.getOriginalUrl();

        // 7. Calculate cache TTL
        Duration ttl = DEFAULT_CACHE_TTL;

        if (expiresAt != null) {

            Duration remaining = Duration.between(
                    now,
                    expiresAt
            );

            if (remaining.compareTo(ttl) < 0) {
                ttl = remaining;
            }
        }

        // 8. Store URL in Redis with TTL
        if (!ttl.isZero() && !ttl.isNegative()) {

            redisTemplate.opsForValue().set(
                    cacheKey,
                    originalUrl,
                    ttl
            );
        }

        return originalUrl;
    }

    // Validate original URL
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
