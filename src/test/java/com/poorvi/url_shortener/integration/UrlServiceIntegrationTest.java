
package com.poorvi.url_shortener.integration;

import com.poorvi.url_shortener.url.dto.UrlResponse;
import com.poorvi.url_shortener.url.entity.ShortUrl;
import com.poorvi.url_shortener.url.exception.ExpiredLinkException;
import com.poorvi.url_shortener.url.exception.ShortUrlNotFoundException;
import com.poorvi.url_shortener.url.repository.ShortUrlRepository;
import com.poorvi.url_shortener.url.service.UrlService;
import com.poorvi.url_shortener.user.entity.user;
import com.poorvi.url_shortener.user.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UrlServiceIntegrationTest {

    @Autowired
    private UrlService urlService;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private user testUser;
    private static final String EMAIL = "integration@test.com";
    private static final String ANOTHER_EMAIL = "another-integration@test.com";

    private String code1;
    private String code2;
    private String code3;
    private String code4;
    private String otherUsersCode;

    @BeforeEach
    void setUp() {

        testUser = findOrCreateUser(EMAIL, "Integration Test User");

        findOrCreateUser(ANOTHER_EMAIL, "Another Integration User");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL, null, List.of()
                )
        );

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);

        code1 = "IT" + suffix + "1";
        code2 = "IT" + suffix + "2";
        code3 = "IT" + suffix + "3";
        code4 = "IT" + suffix + "4";

        otherUsersCode = "ITOWNER" + suffix;
    }

    private user findOrCreateUser(String email, String name) {

        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    user newUser = new user();
                    newUser.setName(name);
                    newUser.setEmail(email);
                    newUser.setPassword("test_password");
                    return userRepository.saveAndFlush(newUser);
                });
    }

    @AfterEach
    void tearDown() {

        List.of(code1, code2, code3, code4, otherUsersCode)
                .forEach(code -> {
                    redisTemplate.delete("url:" + code);
                    shortUrlRepository.findByShortCode(code)
                            .ifPresent(shortUrlRepository::delete);
                });

        SecurityContextHolder.clearContext();
    }

    // 1. Create short URL and verify PostgreSQL persistence
    @Test
    void shouldCreateShortUrlInPostgreSQL() {

        UrlResponse response = urlService.createShortUrl(
                "https://www.google.com", null
        );

        assertNotNull(response);

        String shortCode = response.getShortUrl()
                .substring(response.getShortUrl().lastIndexOf("/") + 1);

        ShortUrl saved = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow();

        assertEquals("https://www.google.com", saved.getOriginalUrl());

        assertTrue(
                shortUrlRepository
                        .findByShortCodeAndOwner_Email(shortCode, EMAIL)
                        .isPresent()
        );

        shortUrlRepository.delete(saved);
    }

    // 2. Retrieve original URL from PostgreSQL
    @Test
    void shouldRetrieveOriginalUrlFromPostgreSQL() {

        shortUrlRepository.saveAndFlush(
                ShortUrl.builder()
                        .originalUrl("https://example.com")
                        .shortCode(code1)
                        .createdAt(LocalDateTime.now())
                        .owner(testUser)
                        .build()
        );

        assertEquals(
                "https://example.com",
                urlService.getOriginalUrl(code1)
        );
    }

    // 3. Verify Redis caching after cache MISS
    @Test
    void shouldCacheRetrievedUrlInRedis() {

        shortUrlRepository.saveAndFlush(
                ShortUrl.builder()
                        .originalUrl("https://example.com")
                        .shortCode(code2)
                        .createdAt(LocalDateTime.now())
                        .owner(testUser)
                        .build()
        );

        urlService.getOriginalUrl(code2);

        assertEquals(
                "https://example.com",
                redisTemplate.opsForValue().get("url:" + code2)
        );

        Long ttl = redisTemplate.getExpire(
                "url:" + code2, TimeUnit.SECONDS
        );

        assertNotNull(ttl);
        assertTrue(ttl > 0);
    }

    // 4. Reject expired URL
    @Test
    void shouldRejectExpiredUrl() {

        shortUrlRepository.saveAndFlush(
                ShortUrl.builder()
                        .originalUrl("https://example.com")
                        .shortCode(code3)
                        .createdAt(LocalDateTime.now().minusHours(1))
                        .expiresAt(LocalDateTime.now().minusMinutes(5))
                        .owner(testUser)
                        .build()
        );

        assertThrows(
                ExpiredLinkException.class,
                () -> urlService.getOriginalUrl(code3)
        );
    }

    // 5. Delete own URL and invalidate Redis cache
    @Test
    void shouldDeleteOwnUrlFromPostgreSQL() {

        shortUrlRepository.saveAndFlush(
                ShortUrl.builder()
                        .originalUrl("https://example.com")
                        .shortCode(code4)
                        .createdAt(LocalDateTime.now())
                        .owner(testUser)
                        .build()
        );

        redisTemplate.opsForValue().set(
                "url:" + code4,
                "https://example.com",
                Duration.ofMinutes(10)
        );

        urlService.deleteShortUrl(code4);

        assertFalse(
                shortUrlRepository.findByShortCode(code4).isPresent()
        );

        assertNull(
                redisTemplate.opsForValue().get("url:" + code4)
        );
    }

    // 6. Verify Redis cache HIT
    @Test
    void shouldReturnUrlFromRedisCacheHit() {

        String cachedUrl = "https://cached-example.com";

        // Save a fallback record in PostgreSQL
        shortUrlRepository.saveAndFlush(
                ShortUrl.builder()
                        .originalUrl(cachedUrl)
                        .shortCode(code4)
                        .createdAt(LocalDateTime.now())
                        .owner(testUser)
                        .build()
        );

        // Store URL in Redis with a valid TTL
        redisTemplate.opsForValue().set(
                "url:" + code4,
                cachedUrl,
                Duration.ofMinutes(10)
        );

        Long ttl = redisTemplate.getExpire(
                "url:" + code4,
                TimeUnit.SECONDS
        );

        assertNotNull(ttl);
        assertTrue(ttl > 0);

        String result = urlService.getOriginalUrl(code4);

        assertEquals(cachedUrl, result);
    }

    // 7. Reject expired URL even when cached in Redis
    @Test
    void shouldRejectExpiredUrlEvenWhenCachedInRedis() {

        String expiredUrl = "https://expired-example.com";

        shortUrlRepository.saveAndFlush(
                ShortUrl.builder()
                        .originalUrl(expiredUrl)
                        .shortCode(code1)
                        .createdAt(LocalDateTime.now().minusHours(2))
                        .expiresAt(LocalDateTime.now().minusMinutes(5))
                        .owner(testUser)
                        .build()
        );

        // Simulate an existing Redis entry without TTL
        redisTemplate.opsForValue().set(
                "url:" + code1,
                expiredUrl
        );

        assertThrows(
                ExpiredLinkException.class,
                () -> urlService.getOriginalUrl(code1)
        );
    }

    // 8. Prevent another user from deleting the URL
    @Test
    void shouldNotDeleteAnotherUsersUrl() {

        shortUrlRepository.saveAndFlush(
                ShortUrl.builder()
                        .originalUrl("https://example.com")
                        .shortCode(otherUsersCode)
                        .createdAt(LocalDateTime.now())
                        .owner(testUser)
                        .build()
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        ANOTHER_EMAIL, null, List.of()
                )
        );

        assertThrows(
                ShortUrlNotFoundException.class,
                () -> urlService.deleteShortUrl(otherUsersCode)
        );

        assertTrue(
                shortUrlRepository.findByShortCode(otherUsersCode).isPresent()
        );
    }
}