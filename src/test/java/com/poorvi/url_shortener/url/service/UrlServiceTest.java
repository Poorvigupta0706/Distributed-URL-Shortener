
package com.poorvi.url_shortener.url.service;

import com.poorvi.url_shortener.url.dto.UrlResponse;
import com.poorvi.url_shortener.url.entity.ShortUrl;
import com.poorvi.url_shortener.url.exception.ExpiredLinkException;
import com.poorvi.url_shortener.url.exception.InvalidUrlException;
import com.poorvi.url_shortener.url.exception.ShortUrlNotFoundException;
import com.poorvi.url_shortener.url.repository.ShortUrlRepository;
import com.poorvi.url_shortener.user.entity.user;
import com.poorvi.url_shortener.user.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UrlServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlService urlService;

    private user testUser;

    @BeforeEach
    void setUp() {

        testUser = mock(user.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@example.com",
                        "password",
                        List.of()
                )
        );

        lenient().when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // 1. Create short URL successfully
    @Test
    void shouldCreateShortUrl() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));

        when(repository.saveAndFlush(any(ShortUrl.class)))
                .thenAnswer(invocation -> {
                    ShortUrl url = invocation.getArgument(0);
                    ReflectionTestUtils.setField(url, "id", 62L);
                    return url;
                });

        when(repository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UrlResponse response = urlService.createShortUrl(
                "https://www.google.com",
                null
        );

        assertNotNull(response);

        verify(userRepository).findByEmail("test@example.com");
        verify(repository).saveAndFlush(any(ShortUrl.class));
        verify(repository).save(any(ShortUrl.class));
    }

    // 2. Reject invalid URL
    @Test
    void shouldRejectInvalidUrl() {

        assertThrows(
                InvalidUrlException.class,
                () -> urlService.createShortUrl("invalid-url", null)
        );

        verifyNoInteractions(repository, userRepository);
    }

    // 3. Reject past expiration date
    @Test
    void shouldRejectPastExpirationDate() {

        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        assertThrows(
                InvalidUrlException.class,
                () -> urlService.createShortUrl(
                        "https://www.google.com",
                        pastDate
                )
        );

        verifyNoInteractions(repository, userRepository);
    }

    // 4. Return URL from Redis cache without database call
    @Test
    void shouldReturnCachedUrlWithoutDatabaseCall() {

        String shortCode = "10";
        String originalUrl = "https://www.google.com";

        when(valueOperations.get("url:" + shortCode))
                .thenReturn(originalUrl);

        // Redis entry has a valid positive TTL
        when(redisTemplate.getExpire(
                "url:" + shortCode,
                TimeUnit.MILLISECONDS
        )).thenReturn(60000L);

        String result = urlService.getOriginalUrl(shortCode);

        assertEquals(originalUrl, result);

        verify(valueOperations).get("url:" + shortCode);

        verify(redisTemplate).getExpire(
                "url:" + shortCode,
                TimeUnit.MILLISECONDS
        );

        verifyNoInteractions(repository);
    }

    // 5. Fetch URL from database when cache misses
    @Test
    void shouldFetchUrlFromDatabaseWhenCacheMisses() {

        String shortCode = "10";
        String originalUrl = "https://www.google.com";

        ShortUrl shortUrl = ShortUrl.builder()
                .id(10L)
                .shortCode(shortCode)
                .originalUrl(originalUrl)
                .build();

        when(valueOperations.get("url:" + shortCode))
                .thenReturn(null);

        when(repository.findByShortCode(shortCode))
                .thenReturn(Optional.of(shortUrl));

        String result = urlService.getOriginalUrl(shortCode);

        assertEquals(originalUrl, result);

        verify(repository).findByShortCode(shortCode);

        verify(valueOperations).set(
                eq("url:" + shortCode),
                eq(originalUrl),
                any(Duration.class)
        );
    }

    // 6. Throw exception when short code does not exist
    @Test
    void shouldThrowWhenShortCodeDoesNotExist() {

        String shortCode = "999";

        when(valueOperations.get("url:" + shortCode))
                .thenReturn(null);

        when(repository.findByShortCode(shortCode))
                .thenReturn(Optional.empty());

        assertThrows(
                ShortUrlNotFoundException.class,
                () -> urlService.getOriginalUrl(shortCode)
        );

        verify(repository).findByShortCode(shortCode);
    }

    // 7. Reject expired URL
    @Test
    void shouldRejectExpiredUrl() {

        String shortCode = "10";

        ShortUrl expiredUrl = ShortUrl.builder()
                .id(10L)
                .shortCode(shortCode)
                .originalUrl("https://www.google.com")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(valueOperations.get("url:" + shortCode))
                .thenReturn(null);

        when(repository.findByShortCode(shortCode))
                .thenReturn(Optional.of(expiredUrl));

        assertThrows(
                ExpiredLinkException.class,
                () -> urlService.getOriginalUrl(shortCode)
        );

        verify(repository).findByShortCode(shortCode);

        verify(valueOperations, never()).set(
                anyString(),
                anyString(),
                any(Duration.class)
        );
    }

    // 8. Delete own URL and invalidate Redis after commit
    @Test
    void shouldDeleteOwnShortUrlAndInvalidateCacheAfterCommit() {

        String shortCode = "10";
        String email = "test@example.com";

        ShortUrl shortUrl = ShortUrl.builder()
                .id(10L)
                .originalUrl("https://www.google.com")
                .shortCode(shortCode)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        "password",
                        List.of()
                )
        );

        when(repository.findByShortCodeAndOwner_Email(
                shortCode, email
        )).thenReturn(Optional.of(shortUrl));

        TransactionSynchronizationManager.initSynchronization();

        try {

            urlService.deleteShortUrl(shortCode);

            verify(repository).delete(shortUrl);

            verify(redisTemplate, never())
                    .delete("url:" + shortCode);

            // Simulate transaction commit
            for (TransactionSynchronization synchronization :
                    TransactionSynchronizationManager.getSynchronizations()) {

                synchronization.afterCommit();
            }

            verify(redisTemplate).delete("url:" + shortCode);

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // 9. Prevent one user from deleting another user's URL
    @Test
    void shouldNotAllowUserToDeleteAnotherUsersUrl() {

        String shortCode = "10";
        String userBEmail = "userB@example.com";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userBEmail,
                        "password",
                        List.of()
                )
        );

        when(repository.findByShortCodeAndOwner_Email(
                shortCode, userBEmail
        )).thenReturn(Optional.empty());

        assertThrows(
                ShortUrlNotFoundException.class,
                () -> urlService.deleteShortUrl(shortCode)
        );

        verify(repository, never()).delete(any(ShortUrl.class));

        verify(redisTemplate, never()).delete(anyString());
    }
}
